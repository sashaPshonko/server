package dev.narek.pveauction.db;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.model.ClanData;
import dev.narek.pveauction.model.ClanMember;
import dev.narek.pveauction.model.ClanRole;
import dev.narek.pveauction.model.SavedLocation;
import org.bukkit.Location;

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

public final class ClanRepository {

    private final PveAuctionPlugin plugin;
    private Connection connection;

    public ClanRepository(PveAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "pve.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement st = connection.createStatement()) {
                migrateClanColumns(st);
                st.execute("""
                    CREATE TABLE IF NOT EXISTS clan_members (
                        clan_id INTEGER NOT NULL,
                        player_uuid TEXT PRIMARY KEY,
                        role TEXT NOT NULL,
                        permissions INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (clan_id) REFERENCES clans(id)
                    )
                    """);
                st.execute("""
                    CREATE TABLE IF NOT EXISTS clan_invites (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        clan_id INTEGER NOT NULL,
                        inviter_uuid TEXT NOT NULL,
                        target_uuid TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        expires_at INTEGER NOT NULL
                    )
                    """);
                migratePlayerHomeColumns(st);
            }
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("Clan DB init failed", e);
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

    public int createClan(String name, UUID ownerUuid) throws SQLException {
        long now = System.currentTimeMillis();
        String sql = "INSERT INTO clans (name, owner_uuid, created_at, balance) VALUES (?, ?, ?, 0)";
        int clanId;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, ownerUuid.toString());
            ps.setLong(3, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No clan id");
                }
                clanId = keys.getInt(1);
            }
        }
        addMember(clanId, ownerUuid, ClanRole.OWNER, 0);
        setPlayerClan(ownerUuid, clanId);
        return clanId;
    }

    public void addMember(int clanId, UUID playerUuid, ClanRole role, int permissions) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO clan_members (clan_id, player_uuid, role, permissions)
            VALUES (?, ?, ?, ?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            ps.setString(2, playerUuid.toString());
            ps.setString(3, role.name());
            ps.setInt(4, permissions);
            ps.executeUpdate();
        }
        setPlayerClan(playerUuid, clanId);
    }

    public void removeMember(UUID playerUuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM clan_members WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            ps.executeUpdate();
        }
        setPlayerClan(playerUuid, null);
    }

    public void setPlayerClan(UUID uuid, Integer clanId) throws SQLException {
        String sql = "UPDATE players SET clan_id = ?, updated_at = ? WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (clanId == null) {
                ps.setNull(1, java.sql.Types.INTEGER);
            } else {
                ps.setInt(1, clanId);
            }
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, uuid.toString());
            ps.executeUpdate();
        }
    }

    public Optional<ClanData> findClan(int clanId) throws SQLException {
        String sql = "SELECT id, name, owner_uuid, balance, home_world, home_x, home_y, home_z, home_yaw, home_pitch FROM clans WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapClan(rs));
            }
        }
    }

    public Optional<ClanData> findClanByName(String name) throws SQLException {
        String sql = "SELECT id, name, owner_uuid, balance, home_world, home_x, home_y, home_z, home_yaw, home_pitch FROM clans WHERE LOWER(name) = LOWER(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapClan(rs));
            }
        }
    }

    public Optional<ClanMember> findMember(UUID uuid) throws SQLException {
        String sql = """
            SELECT m.clan_id, m.player_uuid, m.role, m.permissions, p.last_name
            FROM clan_members m
            LEFT JOIN players p ON p.uuid = m.player_uuid
            WHERE m.player_uuid = ?
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapMember(rs));
            }
        }
    }

    public List<ClanMember> listMembers(int clanId) throws SQLException {
        String sql = """
            SELECT m.clan_id, m.player_uuid, m.role, m.permissions, p.last_name
            FROM clan_members m
            LEFT JOIN players p ON p.uuid = m.player_uuid
            WHERE m.clan_id = ?
            ORDER BY CASE m.role WHEN 'OWNER' THEN 0 ELSE 1 END, p.last_name
            """;
        List<ClanMember> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapMember(rs));
                }
            }
        }
        return out;
    }

    public void setPermissions(UUID target, int permissions) throws SQLException {
        String sql = "UPDATE clan_members SET permissions = ? WHERE player_uuid = ? AND role != 'OWNER'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, permissions);
            ps.setString(2, target.toString());
            ps.executeUpdate();
        }
    }

    public void addBalance(int clanId, long amount) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE clans SET balance = balance + ? WHERE id = ?")) {
            ps.setLong(1, amount);
            ps.setInt(2, clanId);
            ps.executeUpdate();
        }
    }

    public boolean withdrawBalance(int clanId, long amount) throws SQLException {
        connection.setAutoCommit(false);
        try {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE clans SET balance = balance - ? WHERE id = ? AND balance >= ?")) {
                ps.setLong(1, amount);
                ps.setInt(2, clanId);
                ps.setLong(3, amount);
                if (ps.executeUpdate() == 0) {
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

    public void clearClanHome(int clanId) throws SQLException {
        String sql = """
            UPDATE clans SET home_world = NULL, home_x = NULL, home_y = NULL, home_z = NULL, home_yaw = NULL, home_pitch = NULL
            WHERE id = ?
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            ps.executeUpdate();
        }
    }

    /** Удаляет клан и все связанные записи. Возвращает UUID бывших участников. */
    public List<UUID> disbandClan(int clanId) throws SQLException {
        List<UUID> members = new ArrayList<>();
        for (ClanMember m : listMembers(clanId)) {
            members.add(m.playerUuid());
        }
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM clan_invites WHERE clan_id = ?")) {
            ps.setInt(1, clanId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM clan_members WHERE clan_id = ?")) {
            ps.setInt(1, clanId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement("UPDATE players SET clan_id = NULL, updated_at = ? WHERE clan_id = ?")) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setInt(2, clanId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM clans WHERE id = ?")) {
            ps.setInt(1, clanId);
            ps.executeUpdate();
        }
        return members;
    }

    public void setClanHome(int clanId, Location loc) throws SQLException {
        String sql = """
            UPDATE clans SET home_world = ?, home_x = ?, home_y = ?, home_z = ?, home_yaw = ?, home_pitch = ?
            WHERE id = ?
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, loc.getWorld().getName());
            ps.setDouble(2, loc.getX());
            ps.setDouble(3, loc.getY());
            ps.setDouble(4, loc.getZ());
            ps.setFloat(5, loc.getYaw());
            ps.setFloat(6, loc.getPitch());
            ps.setInt(7, clanId);
            ps.executeUpdate();
        }
    }

    public long createInvite(int clanId, UUID inviter, UUID target, long ttlMs) throws SQLException {
        long now = System.currentTimeMillis();
        String sql = """
            INSERT INTO clan_invites (clan_id, inviter_uuid, target_uuid, created_at, expires_at)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, clanId);
            ps.setString(2, inviter.toString());
            ps.setString(3, target.toString());
            ps.setLong(4, now);
            ps.setLong(5, now + ttlMs);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("No invite id");
    }

    public void deleteInvitesForTarget(UUID target) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM clan_invites WHERE target_uuid = ?")) {
            ps.setString(1, target.toString());
            ps.executeUpdate();
        }
    }

    public Optional<InviteRow> findActiveInviteForTarget(UUID target) throws SQLException {
        long now = System.currentTimeMillis();
        String sql = """
            SELECT i.id, i.clan_id, i.inviter_uuid, c.name
            FROM clan_invites i
            JOIN clans c ON c.id = i.clan_id
            WHERE i.target_uuid = ? AND i.expires_at > ?
            ORDER BY i.created_at DESC
            LIMIT 1
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, target.toString());
            ps.setLong(2, now);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new InviteRow(
                        rs.getLong("id"),
                        rs.getInt("clan_id"),
                        UUID.fromString(rs.getString("inviter_uuid")),
                        rs.getString("name")
                ));
            }
        }
    }

    public void deleteInvite(long inviteId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM clan_invites WHERE id = ?")) {
            ps.setLong(1, inviteId);
            ps.executeUpdate();
        }
    }

    public void savePlayerHome(UUID uuid, Location loc) throws SQLException {
        String sql = """
            UPDATE players SET home_world = ?, home_x = ?, home_y = ?, home_z = ?, home_yaw = ?, home_pitch = ?, updated_at = ?
            WHERE uuid = ?
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, loc.getWorld().getName());
            ps.setDouble(2, loc.getX());
            ps.setDouble(3, loc.getY());
            ps.setDouble(4, loc.getZ());
            ps.setFloat(5, loc.getYaw());
            ps.setFloat(6, loc.getPitch());
            ps.setLong(7, System.currentTimeMillis());
            ps.setString(8, uuid.toString());
            ps.executeUpdate();
        }
    }

    public Optional<SavedLocation> findPlayerHome(UUID uuid) throws SQLException {
        String sql = "SELECT home_world, home_x, home_y, home_z, home_yaw, home_pitch FROM players WHERE uuid = ? AND home_world IS NOT NULL";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new SavedLocation(
                        rs.getString("home_world"),
                        rs.getDouble("home_x"),
                        rs.getDouble("home_y"),
                        rs.getDouble("home_z"),
                        rs.getFloat("home_yaw"),
                        rs.getFloat("home_pitch")
                ));
            }
        }
    }

    public record InviteRow(long id, int clanId, UUID inviterUuid, String clanName) {}

    private static ClanData mapClan(ResultSet rs) throws SQLException {
        String homeWorld = rs.getString("home_world");
        Double hx = rs.getObject("home_x") == null ? null : rs.getDouble("home_x");
        Double hy = rs.getObject("home_y") == null ? null : rs.getDouble("home_y");
        Double hz = rs.getObject("home_z") == null ? null : rs.getDouble("home_z");
        Float yaw = rs.getObject("home_yaw") == null ? null : rs.getFloat("home_yaw");
        Float pitch = rs.getObject("home_pitch") == null ? null : rs.getFloat("home_pitch");
        return new ClanData(
                rs.getInt("id"),
                rs.getString("name"),
                UUID.fromString(rs.getString("owner_uuid")),
                rs.getLong("balance"),
                homeWorld,
                hx,
                hy,
                hz,
                yaw,
                pitch
        );
    }

    private static ClanMember mapMember(ResultSet rs) throws SQLException {
        String name = rs.getString("last_name");
        if (name == null) {
            name = "?";
        }
        return new ClanMember(
                rs.getInt("clan_id"),
                UUID.fromString(rs.getString("player_uuid")),
                name,
                ClanRole.valueOf(rs.getString("role")),
                rs.getInt("permissions")
        );
    }

    private static void migrateClanColumns(Statement st) throws SQLException {
        String[] cols = {
                "ALTER TABLE clans ADD COLUMN balance INTEGER NOT NULL DEFAULT 0",
                "ALTER TABLE clans ADD COLUMN home_world TEXT",
                "ALTER TABLE clans ADD COLUMN home_x REAL",
                "ALTER TABLE clans ADD COLUMN home_y REAL",
                "ALTER TABLE clans ADD COLUMN home_z REAL",
                "ALTER TABLE clans ADD COLUMN home_yaw REAL",
                "ALTER TABLE clans ADD COLUMN home_pitch REAL"
        };
        for (String sql : cols) {
            try {
                st.execute(sql);
            } catch (SQLException ignored) {
            }
        }
    }

    private static void migratePlayerHomeColumns(Statement st) throws SQLException {
        String[] cols = {
                "ALTER TABLE players ADD COLUMN home_world TEXT",
                "ALTER TABLE players ADD COLUMN home_x REAL",
                "ALTER TABLE players ADD COLUMN home_y REAL",
                "ALTER TABLE players ADD COLUMN home_z REAL",
                "ALTER TABLE players ADD COLUMN home_yaw REAL",
                "ALTER TABLE players ADD COLUMN home_pitch REAL"
        };
        for (String sql : cols) {
            try {
                st.execute(sql);
            } catch (SQLException ignored) {
            }
        }
    }
}
