package dev.narek.pveauction.db;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.shop.ClanCategoryProgress;
import dev.narek.pveauction.shop.ShopCategory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public final class ShopRepository {

    private final PveAuctionPlugin plugin;
    private Connection connection;

    public ShopRepository(PveAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "pve.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.execute("""
                    CREATE TABLE IF NOT EXISTS clan_shop_category (
                        clan_id INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        level INTEGER NOT NULL DEFAULT 1,
                        earned_coins INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (clan_id, category)
                    )
                    """);
                try {
                    st.execute("ALTER TABLE clans ADD COLUMN shop_focus_category TEXT");
                } catch (SQLException ignored) {
                }
            }
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("Shop DB init failed", e);
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

    public Optional<String> findFocusCategory(int clanId) throws SQLException {
        String sql = "SELECT shop_focus_category FROM clans WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                String focus = rs.getString("shop_focus_category");
                if (focus == null || focus.isBlank()) {
                    return Optional.empty();
                }
                return Optional.of(focus);
            }
        }
    }

    public void setFocusCategory(int clanId, ShopCategory category) throws SQLException {
        String sql = "UPDATE clans SET shop_focus_category = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, category.id());
            ps.setInt(2, clanId);
            ps.executeUpdate();
        }
    }

    public ClanCategoryProgress getProgress(int clanId, ShopCategory category) throws SQLException {
        String sql = "SELECT level, earned_coins FROM clan_shop_category WHERE clan_id = ? AND category = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            ps.setString(2, category.id());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return ClanCategoryProgress.DEFAULT;
                }
                return new ClanCategoryProgress(rs.getInt("level"), rs.getLong("earned_coins"));
            }
        }
    }

    public void saveProgress(int clanId, ShopCategory category, ClanCategoryProgress progress) throws SQLException {
        String sql = """
            INSERT INTO clan_shop_category (clan_id, category, level, earned_coins)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(clan_id, category) DO UPDATE SET
                level = excluded.level,
                earned_coins = excluded.earned_coins
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            ps.setString(2, category.id());
            ps.setInt(3, progress.level());
            ps.setLong(4, progress.earnedCoins());
            ps.executeUpdate();
        }
    }
}
