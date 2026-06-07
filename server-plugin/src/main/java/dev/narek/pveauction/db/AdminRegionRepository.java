package dev.narek.pveauction.db;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.model.AdminRegion;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class AdminRegionRepository {

    private final PveAuctionPlugin plugin;
    private Connection connection;

    public AdminRegionRepository(PveAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "pve.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.execute("""
                    CREATE TABLE IF NOT EXISTS admin_regions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL UNIQUE COLLATE NOCASE,
                        world TEXT NOT NULL,
                        min_x INTEGER NOT NULL,
                        max_x INTEGER NOT NULL,
                        min_y INTEGER NOT NULL,
                        max_y INTEGER NOT NULL,
                        min_z INTEGER NOT NULL,
                        max_z INTEGER NOT NULL,
                        created_by_uuid TEXT NOT NULL,
                        created_by_name TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """);
                migrateAdminRegionY(st);
                migrateAdminRegionHomeAndInteract(st);
            }
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("Admin regions DB init failed", e);
        }
    }

    private void migrateAdminRegionY(Statement st) throws SQLException {
        if (!columnExists(st, "admin_regions", "min_y")) {
            st.execute("ALTER TABLE admin_regions ADD COLUMN min_y INTEGER NOT NULL DEFAULT -64");
        }
        if (!columnExists(st, "admin_regions", "max_y")) {
            st.execute("ALTER TABLE admin_regions ADD COLUMN max_y INTEGER NOT NULL DEFAULT 320");
        }
    }

    private void migrateAdminRegionHomeAndInteract(Statement st) throws SQLException {
        if (!columnExists(st, "admin_regions", "allow_member_interact")) {
            st.execute("ALTER TABLE admin_regions ADD COLUMN allow_member_interact INTEGER NOT NULL DEFAULT 0");
        }
        if (!columnExists(st, "admin_regions", "home_world")) {
            st.execute("ALTER TABLE admin_regions ADD COLUMN home_world TEXT");
            st.execute("ALTER TABLE admin_regions ADD COLUMN home_x REAL");
            st.execute("ALTER TABLE admin_regions ADD COLUMN home_y REAL");
            st.execute("ALTER TABLE admin_regions ADD COLUMN home_z REAL");
            st.execute("ALTER TABLE admin_regions ADD COLUMN home_yaw REAL");
            st.execute("ALTER TABLE admin_regions ADD COLUMN home_pitch REAL");
        }
    }

    public int countByCreator(UUID uuid) throws SQLException {
        String sql = "SELECT COUNT(*) FROM admin_regions WHERE created_by_uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void setHome(String name, String world, double x, double y, double z, float yaw, float pitch)
            throws SQLException {
        String sql = """
            UPDATE admin_regions SET
                home_world = ?, home_x = ?, home_y = ?, home_z = ?, home_yaw = ?, home_pitch = ?
            WHERE name = ? COLLATE NOCASE
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setDouble(2, x);
            ps.setDouble(3, y);
            ps.setDouble(4, z);
            ps.setFloat(5, yaw);
            ps.setFloat(6, pitch);
            ps.setString(7, name);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("region not found: " + name);
            }
        }
    }

    public void clearHome(String name) throws SQLException {
        String sql = """
            UPDATE admin_regions SET
                home_world = NULL, home_x = NULL, home_y = NULL, home_z = NULL,
                home_yaw = NULL, home_pitch = NULL
            WHERE name = ? COLLATE NOCASE
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.executeUpdate();
        }
    }

    public void setAllowMemberInteract(String name, boolean allowed) throws SQLException {
        String sql = "UPDATE admin_regions SET allow_member_interact = ? WHERE name = ? COLLATE NOCASE";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, allowed ? 1 : 0);
            ps.setString(2, name);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("region not found: " + name);
            }
        }
    }

    private static boolean columnExists(Statement st, String table, String column) throws SQLException {
        try (ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }

    public List<AdminRegion> listAll() throws SQLException {
        String sql = "SELECT * FROM admin_regions ORDER BY name COLLATE NOCASE";
        List<AdminRegion> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(mapRow(rs));
            }
        }
        return out;
    }

    public Optional<AdminRegion> findByName(String name) throws SQLException {
        String sql = "SELECT * FROM admin_regions WHERE name = ? COLLATE NOCASE";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        }
    }

    public Optional<AdminRegion> findAt(String world, int x, int y, int z) throws SQLException {
        String sql = """
            SELECT * FROM admin_regions
            WHERE world = ?
              AND ? BETWEEN min_x AND max_x
              AND ? BETWEEN min_y AND max_y
              AND ? BETWEEN min_z AND max_z
            LIMIT 1
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        }
    }

    public void insert(
            String name,
            String world,
            int minX,
            int maxX,
            int minY,
            int maxY,
            int minZ,
            int maxZ,
            UUID creator,
            String creatorName
    ) throws SQLException {
        String sql = """
            INSERT INTO admin_regions (
                name, world, min_x, max_x, min_y, max_y, min_z, max_z,
                created_by_uuid, created_by_name, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name.toLowerCase(Locale.ROOT));
            ps.setString(2, world);
            ps.setInt(3, minX);
            ps.setInt(4, maxX);
            ps.setInt(5, minY);
            ps.setInt(6, maxY);
            ps.setInt(7, minZ);
            ps.setInt(8, maxZ);
            ps.setString(9, creator.toString());
            ps.setString(10, creatorName);
            ps.setLong(11, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public boolean deleteByName(String name) throws SQLException {
        String sql = "DELETE FROM admin_regions WHERE name = ? COLLATE NOCASE";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            return ps.executeUpdate() > 0;
        }
    }

    private static AdminRegion mapRow(ResultSet rs) throws SQLException {
        return new AdminRegion(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("world"),
                rs.getInt("min_x"),
                rs.getInt("max_x"),
                rs.getInt("min_y"),
                rs.getInt("max_y"),
                rs.getInt("min_z"),
                rs.getInt("max_z"),
                UUID.fromString(rs.getString("created_by_uuid")),
                rs.getString("created_by_name"),
                rs.getLong("created_at"),
                rs.getInt("allow_member_interact") != 0,
                rs.getString("home_world"),
                readDouble(rs, "home_x"),
                readDouble(rs, "home_y"),
                readDouble(rs, "home_z"),
                readFloat(rs, "home_yaw"),
                readFloat(rs, "home_pitch")
        );
    }

    private static Double readDouble(ResultSet rs, String column) throws SQLException {
        double v = rs.getDouble(column);
        return rs.wasNull() ? null : v;
    }

    private static Float readFloat(ResultSet rs, String column) throws SQLException {
        float v = rs.getFloat(column);
        return rs.wasNull() ? null : v;
    }
}
