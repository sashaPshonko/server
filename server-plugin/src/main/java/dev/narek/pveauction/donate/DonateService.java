package dev.narek.pveauction.donate;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.db.DonateRepository;
import dev.narek.pveauction.model.PlayerDonate;
import dev.narek.pveauction.util.RankColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Иерархия донатов (1–11): в игре действует только самый высокий из не истёкших.
 * Слоты АХ: база (5) + уровень доната (герцог 6, горбатый 7, …, готка 16).
 */
public final class DonateService {

    private final PveAuctionPlugin plugin;
    private final DonateRepository repo;
    private final ConcurrentHashMap<UUID, List<PlayerDonate>> cache = new ConcurrentHashMap<>();

    public DonateService(PveAuctionPlugin plugin, DonateRepository repo) {
        this.plugin = plugin;
        this.repo = repo;
    }

    public List<PlayerDonate> active(UUID playerId) throws SQLException {
        List<PlayerDonate> cached = cache.get(playerId);
        if (cached != null) {
            return cached;
        }
        List<PlayerDonate> loaded = repo.listActive(playerId);
        cache.put(playerId, loaded);
        return loaded;
    }

    /** Донат с максимальным приоритетом (sort_order) среди не истёкших. */
    public Optional<PlayerDonate> primaryActive(UUID playerId) throws SQLException {
        return active(playerId).stream()
                .max(Comparator.comparingInt(PlayerDonate::sortOrder));
    }

    public void invalidate(UUID playerId) {
        cache.remove(playerId);
    }

    public int maxActiveLots(UUID playerId) throws SQLException {
        int base = plugin.getConfig().getInt("max-active-lots", 5);
        return primaryActive(playerId)
                .map(d -> base + d.sortOrder())
                .orElse(base);
    }

    public static Component donateTag(PlayerDonate donate) {
        NamedTextColor color = RankColors.parse(donate.color());
        return Component.text("[" + donate.displayName() + "] ", color);
    }

    public static Component donateRankName(PlayerDonate donate) {
        return Component.text(donate.displayName(), RankColors.parse(donate.color()));
    }

    public GrantResult grant(UUID targetId, DonateType type, String durationRaw, String grantedBy) throws SQLException {
        Long expiresAt = parseDuration(durationRaw);
        if (expiresAt == null && !isPermanentToken(durationRaw)) {
            return GrantResult.badDuration();
        }
        if (isPermanentToken(durationRaw)) {
            expiresAt = null;
            repo.clearPermanent(targetId);
        }
        repo.grant(targetId, type, expiresAt, grantedBy);
        invalidate(targetId);
        return GrantResult.ok(type, expiresAt);
    }

    public void purgeExpiredAndRefresh() {
        try {
            int removed = repo.purgeExpired();
            if (removed > 0) {
                cache.clear();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (plugin.scoreboardListener() != null) {
                        for (Player online : Bukkit.getOnlinePlayers()) {
                            plugin.scoreboardListener().refresh(online);
                        }
                    }
                });
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Донаты: очистка истёкших — " + e.getMessage());
        }
    }

    private static Long parseDuration(String raw) {
        if (raw == null || raw.isBlank() || isPermanentToken(raw)) {
            return null;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        try {
            if (s.endsWith("d") || s.endsWith("д")) {
                long days = Long.parseLong(s.substring(0, s.length() - 1).trim());
                return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days);
            }
            long days = Long.parseLong(s);
            return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isPermanentToken(String raw) {
        if (raw == null) {
            return false;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        return s.equals("forever")
                || s.equals("navsegda")
                || s.equals("permanent")
                || s.equals("0")
                || s.equals("-1");
    }

    public record GrantResult(boolean success, String error, DonateType type, Long expiresAt) {
        static GrantResult ok(DonateType type, Long expiresAt) {
            return new GrantResult(true, null, type, expiresAt);
        }

        static GrantResult badDuration() {
            return new GrantResult(false, "Срок: число дней, 30d, forever или navsegda", null, null);
        }
    }

    public int remove(UUID playerId, String donateId) throws SQLException {
        int n = repo.remove(playerId, donateId);
        if (n > 0) {
            invalidate(playerId);
        }
        return n;
    }

    public void refreshOnlinePlayer(UUID playerId) {
        invalidate(playerId);
        Player online = Bukkit.getPlayer(playerId);
        if (online != null && online.isOnline() && plugin.scoreboardListener() != null) {
            plugin.scoreboardListener().refresh(online);
        }
    }
}
