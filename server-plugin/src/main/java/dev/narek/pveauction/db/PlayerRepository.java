package dev.narek.pveauction.db;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.model.PlayerProfile;
import dev.narek.pveauction.model.SavedLocation;
import org.bukkit.Location;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;

public final class PlayerRepository {

    private final PveAuctionPlugin plugin;
    private Connection connection;

    public PlayerRepository(PveAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "pve.db");
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                throw new SQLException("Не создать папку данных");
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.execute("""
                    CREATE TABLE IF NOT EXISTS ranks (
                        id TEXT PRIMARY KEY,
                        display_name TEXT NOT NULL,
                        color TEXT NOT NULL DEFAULT 'GREEN',
                        sort_order INTEGER NOT NULL DEFAULT 0
                    )
                    """);
                migrateRankColorColumn(st);
                st.execute("""
                    CREATE TABLE IF NOT EXISTS clans (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL UNIQUE,
                        tag TEXT,
                        owner_uuid TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """);
                st.execute("""
                    CREATE TABLE IF NOT EXISTS players (
                        uuid TEXT PRIMARY KEY,
                        last_name TEXT NOT NULL,
                        rank_id TEXT NOT NULL DEFAULT 'player',
                        tokens INTEGER NOT NULL DEFAULT 0,
                        clan_id INTEGER,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        FOREIGN KEY (rank_id) REFERENCES ranks(id),
                        FOREIGN KEY (clan_id) REFERENCES clans(id)
                    )
                    """);
                st.execute("""
                    INSERT OR IGNORE INTO ranks (id, display_name, color, sort_order)
                    VALUES ('player', 'Игрок', 'GREEN', 0)
                    """);
                st.execute("UPDATE ranks SET color = 'GREEN' WHERE id = 'player' AND (color IS NULL OR color = '')");
                migrateLogoutColumns(st);
            }
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("pve.db init failed", e);
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

    public PlayerProfile getOrCreate(UUID uuid, String name) throws SQLException {
        Optional<PlayerProfile> existing = find(uuid);
        if (existing.isPresent()) {
            touchName(uuid, name);
            return find(uuid).orElseThrow();
        }
        long now = System.currentTimeMillis();
        String sql = """
            INSERT INTO players (uuid, last_name, rank_id, tokens, clan_id, created_at, updated_at)
            VALUES (?, ?, 'player', 0, NULL, ?, ?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setLong(3, now);
            ps.setLong(4, now);
            ps.executeUpdate();
        }
        return find(uuid).orElseThrow();
    }

    public Optional<PlayerProfile> find(UUID uuid) throws SQLException {
        String sql = """
            SELECT p.uuid, p.last_name, p.rank_id, r.display_name, r.color, p.tokens, p.clan_id, c.name
            FROM players p
            JOIN ranks r ON r.id = p.rank_id
            LEFT JOIN clans c ON c.id = p.clan_id
            WHERE p.uuid = ?
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        }
    }

    public void saveLogoutLocation(UUID uuid, Location location) throws SQLException {
        String sql = """
            UPDATE players SET
                logout_world = ?,
                logout_x = ?,
                logout_y = ?,
                logout_z = ?,
                logout_yaw = ?,
                logout_pitch = ?,
                updated_at = ?
            WHERE uuid = ?
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, location.getWorld().getName());
            ps.setDouble(2, location.getX());
            ps.setDouble(3, location.getY());
            ps.setDouble(4, location.getZ());
            ps.setFloat(5, location.getYaw());
            ps.setFloat(6, location.getPitch());
            ps.setLong(7, System.currentTimeMillis());
            ps.setString(8, uuid.toString());
            ps.executeUpdate();
        }
    }

    public Optional<SavedLocation> findLogoutLocation(UUID uuid) throws SQLException {
        String sql = """
            SELECT logout_world, logout_x, logout_y, logout_z, logout_yaw, logout_pitch
            FROM players
            WHERE uuid = ? AND logout_world IS NOT NULL
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new SavedLocation(
                        rs.getString("logout_world"),
                        rs.getDouble("logout_x"),
                        rs.getDouble("logout_y"),
                        rs.getDouble("logout_z"),
                        rs.getFloat("logout_yaw"),
                        rs.getFloat("logout_pitch")
                ));
            }
        }
    }

    private void touchName(UUID uuid, String name) throws SQLException {
        String sql = "UPDATE players SET last_name = ?, updated_at = ? WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, uuid.toString());
            ps.executeUpdate();
        }
    }

    private static void migrateRankColorColumn(Statement st) throws SQLException {
        try {
            st.execute("ALTER TABLE ranks ADD COLUMN color TEXT NOT NULL DEFAULT 'GREEN'");
        } catch (SQLException ignored) {
            // колонка уже есть
        }
    }

    private static void migrateLogoutColumns(Statement st) throws SQLException {
        String[] alters = {
                "ALTER TABLE players ADD COLUMN logout_world TEXT",
                "ALTER TABLE players ADD COLUMN logout_x REAL",
                "ALTER TABLE players ADD COLUMN logout_y REAL",
                "ALTER TABLE players ADD COLUMN logout_z REAL",
                "ALTER TABLE players ADD COLUMN logout_yaw REAL",
                "ALTER TABLE players ADD COLUMN logout_pitch REAL"
        };
        for (String sql : alters) {
            try {
                st.execute(sql);
            } catch (SQLException ignored) {
                // колонка уже есть
            }
        }
    }

    private static PlayerProfile mapRow(ResultSet rs) throws SQLException {
        int clanIdRaw = rs.getInt("clan_id");
        Integer clanId = rs.wasNull() ? null : clanIdRaw;
        String clanName = rs.getString("name");
        return new PlayerProfile(
                UUID.fromString(rs.getString("uuid")),
                rs.getString("last_name"),
                rs.getString("rank_id"),
                rs.getString("display_name"),
                rs.getString("color"),
                rs.getLong("tokens"),
                clanId,
                clanName
        );
    }
}
