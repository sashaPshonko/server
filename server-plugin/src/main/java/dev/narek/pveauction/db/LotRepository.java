package dev.narek.pveauction.db;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.model.AuctionLot;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class LotRepository {

    private final PveAuctionPlugin plugin;
    private Connection connection;

    public LotRepository(PveAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "auctions.db");
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                throw new SQLException("Не создать папку данных");
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.execute("""
                    CREATE TABLE IF NOT EXISTS lots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        seller_uuid TEXT NOT NULL,
                        seller_name TEXT NOT NULL,
                        item_blob BLOB NOT NULL,
                        price INTEGER NOT NULL,
                        sold INTEGER NOT NULL DEFAULT 0,
                        buyer_uuid TEXT,
                        created_at INTEGER NOT NULL
                    )
                    """);
                st.execute("CREATE INDEX IF NOT EXISTS idx_lots_active ON lots(sold, created_at)");
                st.execute("CREATE INDEX IF NOT EXISTS idx_lots_seller ON lots(seller_uuid, sold)");
            }
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("SQLite init failed", e);
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }

    public long insertLot(UUID sellerUuid, String sellerName, ItemStack item, long price) throws SQLException {
        byte[] blob = itemStackToBytes(item);
        String sql = """
            INSERT INTO lots (seller_uuid, seller_name, item_blob, price, created_at)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sellerUuid.toString());
            ps.setString(2, sellerName);
            ps.setBytes(3, blob);
            ps.setLong(4, price);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Нет id нового лота");
    }

    /** Лоты на витрине аукциона (не проданы, не истекли). */
    public int countListedLots() throws SQLException {
        String sql = "SELECT COUNT(*) FROM lots WHERE sold = 0 AND created_at >= ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, minCreatedAtListed());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /** Все неснятые лоты продавца (включая истёкшие) — лимит слотов. */
    public int countUnsoldBySeller(UUID sellerUuid) throws SQLException {
        String sql = "SELECT COUNT(*) FROM lots WHERE sold = 0 AND seller_uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, sellerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public List<AuctionLot> listListed(int limit, int offset) throws SQLException {
        String sql = """
            SELECT id, seller_uuid, seller_name, item_blob, price, created_at
            FROM lots WHERE sold = 0 AND created_at >= ?
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            """;
        List<AuctionLot> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, minCreatedAtListed());
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
            }
        }
        return out;
    }

    /** Хранилище: все неснятые лоты, в т.ч. истёкшие. */
    public List<AuctionLot> listUnsoldBySeller(UUID sellerUuid, int limit) throws SQLException {
        String sql = """
            SELECT id, seller_uuid, seller_name, item_blob, price, created_at
            FROM lots WHERE sold = 0 AND seller_uuid = ?
            ORDER BY created_at DESC
            LIMIT ?
            """;
        List<AuctionLot> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, sellerUuid.toString());
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
            }
        }
        return out;
    }

    public Optional<AuctionLot> findListed(long id) throws SQLException {
        String sql = """
            SELECT id, seller_uuid, seller_name, item_blob, price, created_at
            FROM lots WHERE id = ? AND sold = 0 AND created_at >= ?
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, minCreatedAtListed());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<AuctionLot> findUnsold(long id, UUID sellerUuid) throws SQLException {
        String sql = """
            SELECT id, seller_uuid, seller_name, item_blob, price, created_at
            FROM lots WHERE id = ? AND sold = 0 AND seller_uuid = ?
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setString(2, sellerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<AuctionLot> cancelActiveLot(long lotId, UUID sellerUuid) throws SQLException {
        Optional<AuctionLot> lot = findUnsold(lotId, sellerUuid);
        if (lot.isEmpty()) {
            return Optional.empty();
        }
        String sql = "DELETE FROM lots WHERE id = ? AND seller_uuid = ? AND sold = 0";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, lotId);
            ps.setString(2, sellerUuid.toString());
            if (ps.executeUpdate() == 0) {
                return Optional.empty();
            }
        }
        return lot;
    }

    /** Перевыставить — сброс таймера (created_at) у всех неснятых лотов. */
    public int relistSellerLots(UUID sellerUuid) throws SQLException {
        String sql = "UPDATE lots SET created_at = ? WHERE seller_uuid = ? AND sold = 0";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setString(2, sellerUuid.toString());
            return ps.executeUpdate();
        }
    }

    public boolean tryMarkSold(long lotId, UUID buyerUuid) throws SQLException {
        connection.setAutoCommit(false);
        try {
            String sql = "UPDATE lots SET sold = 1, buyer_uuid = ? WHERE id = ? AND sold = 0 AND created_at >= ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, buyerUuid.toString());
                ps.setLong(2, lotId);
                ps.setLong(3, minCreatedAtListed());
                int n = ps.executeUpdate();
                if (n == 0) {
                    connection.rollback();
                    return false;
                }
            }
            connection.commit();
            return true;
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public ItemStack bytesToItem(byte[] blob) {
        return ItemStack.deserializeBytes(blob);
    }

    private long minCreatedAtListed() {
        return System.currentTimeMillis() - plugin.auctionExpiryMs();
    }

    private static byte[] itemStackToBytes(ItemStack item) {
        return item.serializeAsBytes();
    }

    private static AuctionLot mapRow(ResultSet rs) throws SQLException {
        return new AuctionLot(
                rs.getLong("id"),
                UUID.fromString(rs.getString("seller_uuid")),
                rs.getString("seller_name"),
                rs.getBytes("item_blob"),
                rs.getLong("price"),
                rs.getLong("created_at")
        );
    }
}
