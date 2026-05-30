package dev.narek.pveauction.auth;

import dev.narek.pveauction.PveAuctionPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;

public final class AuthRepository {

    private final PveAuctionPlugin plugin;
    private Connection connection;

    public AuthRepository(PveAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "pve.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.execute("""
                    CREATE TABLE IF NOT EXISTS player_auth (
                        uuid TEXT PRIMARY KEY,
                        password_hash TEXT NOT NULL,
                        registered_at INTEGER NOT NULL
                    )
                    """);
                st.execute("""
                    CREATE TABLE IF NOT EXISTS player_auth_sessions (
                        uuid TEXT PRIMARY KEY,
                        ip_address TEXT NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """);
            }
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("auth tables init failed", e);
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

    public boolean isRegistered(UUID uuid) throws SQLException {
        String sql = "SELECT 1 FROM player_auth WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void register(UUID uuid, String passwordHash) throws SQLException {
        long now = System.currentTimeMillis();
        String sql = """
            INSERT INTO player_auth (uuid, password_hash, registered_at)
            VALUES (?, ?, ?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, passwordHash);
            ps.setLong(3, now);
            ps.executeUpdate();
        }
    }

    public Optional<String> findPasswordHash(UUID uuid) throws SQLException {
        String sql = "SELECT password_hash FROM player_auth WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(rs.getString("password_hash"));
            }
        }
    }

    public Optional<String> findSessionIp(UUID uuid) throws SQLException {
        String sql = "SELECT ip_address FROM player_auth_sessions WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(rs.getString("ip_address"));
            }
        }
    }

    /** Одна сессия на игрока; новый IP перезаписывает старый. */
    public void upsertSession(UUID uuid, String ip) throws SQLException {
        long now = System.currentTimeMillis();
        String sql = """
            INSERT INTO player_auth_sessions (uuid, ip_address, updated_at)
            VALUES (?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                ip_address = excluded.ip_address,
                updated_at = excluded.updated_at
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, ip);
            ps.setLong(3, now);
            ps.executeUpdate();
        }
    }
}
