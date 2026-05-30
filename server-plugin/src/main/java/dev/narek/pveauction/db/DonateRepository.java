package dev.narek.pveauction.db;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.donate.DonateType;
import dev.narek.pveauction.model.PlayerDonate;

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

public final class DonateRepository {

    private final PveAuctionPlugin plugin;
    private Connection connection;

    public DonateRepository(PveAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "pve.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.execute("""
                    CREATE TABLE IF NOT EXISTS donate_types (
                        id TEXT PRIMARY KEY,
                        display_name TEXT NOT NULL,
                        color TEXT NOT NULL,
                        sort_order INTEGER NOT NULL DEFAULT 0
                    )
                    """);
                st.execute("""
                    CREATE TABLE IF NOT EXISTS player_donates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_uuid TEXT NOT NULL,
                        donate_id TEXT NOT NULL,
                        expires_at INTEGER,
                        granted_at INTEGER NOT NULL,
                        granted_by TEXT,
                        FOREIGN KEY (donate_id) REFERENCES donate_types(id)
                    )
                    """);
                st.execute("""
                    CREATE INDEX IF NOT EXISTS idx_player_donates_uuid
                    ON player_donates(player_uuid)
                    """);
                migrateKrasivyToDyravy(st);
            }
            seedTypes();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("donate tables init failed", e);
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

    private static void migrateKrasivyToDyravy(Statement st) throws SQLException {
        st.executeUpdate("UPDATE player_donates SET donate_id = 'dyravy' WHERE donate_id = 'krasivy'");
        st.executeUpdate("DELETE FROM donate_types WHERE id = 'krasivy'");
    }

    private void seedTypes() throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO donate_types (id, display_name, color, sort_order)
            VALUES (?, ?, ?, ?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (DonateType type : DonateType.values()) {
                ps.setString(1, type.id());
                ps.setString(2, type.displayName());
                ps.setString(3, type.color());
                ps.setInt(4, type.sortOrder());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<PlayerDonate> listActive(UUID playerId) throws SQLException {
        long now = System.currentTimeMillis();
        String sql = """
            SELECT d.id, d.display_name, d.color, d.sort_order, pd.expires_at, pd.granted_at
            FROM player_donates pd
            JOIN donate_types d ON d.id = pd.donate_id
            WHERE pd.player_uuid = ?
              AND (pd.expires_at IS NULL OR pd.expires_at > ?)
            ORDER BY d.sort_order, pd.granted_at
            """;
        List<PlayerDonate> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setLong(2, now);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long expRaw = rs.getLong("expires_at");
                    Long expiresAt = rs.wasNull() ? null : expRaw;
                    out.add(new PlayerDonate(
                            rs.getString("id"),
                            rs.getString("display_name"),
                            rs.getString("color"),
                            rs.getInt("sort_order"),
                            expiresAt,
                            rs.getLong("granted_at")
                    ));
                }
            }
        }
        return out;
    }

    public void clearPermanent(UUID playerId) throws SQLException {
        String sql = "DELETE FROM player_donates WHERE player_uuid = ? AND expires_at IS NULL";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.executeUpdate();
        }
    }

    public void grant(UUID playerId, DonateType type, Long expiresAt, String grantedBy) throws SQLException {
        if (expiresAt == null) {
            clearPermanent(playerId);
        }
        removeDonate(playerId, type.id());
        String sql = """
            INSERT INTO player_donates (player_uuid, donate_id, expires_at, granted_at, granted_by)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, type.id());
            if (expiresAt == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setLong(3, expiresAt);
            }
            ps.setLong(4, System.currentTimeMillis());
            ps.setString(5, grantedBy);
            ps.executeUpdate();
        }
    }

    public void removeDonate(UUID playerId, String donateId) throws SQLException {
        String sql = "DELETE FROM player_donates WHERE player_uuid = ? AND donate_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, donateId);
            ps.executeUpdate();
        }
    }

    public int remove(UUID playerId, String donateId) throws SQLException {
        String sql = "DELETE FROM player_donates WHERE player_uuid = ? AND donate_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, donateId);
            return ps.executeUpdate();
        }
    }

    public Optional<PlayerDonate> findPermanent(UUID playerId) throws SQLException {
        long now = System.currentTimeMillis();
        String sql = """
            SELECT d.id, d.display_name, d.color, d.sort_order, pd.expires_at, pd.granted_at
            FROM player_donates pd
            JOIN donate_types d ON d.id = pd.donate_id
            WHERE pd.player_uuid = ? AND pd.expires_at IS NULL
            LIMIT 1
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        }
    }

    public int purgeExpired() throws SQLException {
        String sql = "DELETE FROM player_donates WHERE expires_at IS NOT NULL AND expires_at <= ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, System.currentTimeMillis());
            return ps.executeUpdate();
        }
    }

    private static PlayerDonate mapRow(ResultSet rs) throws SQLException {
        long expRaw = rs.getLong("expires_at");
        Long expiresAt = rs.wasNull() ? null : expRaw;
        return new PlayerDonate(
                rs.getString("id"),
                rs.getString("display_name"),
                rs.getString("color"),
                rs.getInt("sort_order"),
                expiresAt,
                rs.getLong("granted_at")
        );
    }
}
