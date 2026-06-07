package dev.narek.pveauction.auth;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.util.TravelMsg;
import dev.narek.pveauction.world.JoinTeleportService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import java.time.Duration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import org.bukkit.scheduler.BukkitTask;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthService {

    private final PveAuctionPlugin plugin;
    private final AuthRepository repository;
    private final Set<UUID> loggedIn = ConcurrentHashMap.newKeySet();
    private final Set<UUID> allowTeleport = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BukkitTask> authTitleRefresh = new ConcurrentHashMap<>();
    private Location authLocation;

    public AuthService(PveAuctionPlugin plugin, AuthRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        reload();
    }

    public void reload() {
        var cfg = plugin.getConfig();
        String worldName = cfg.getString("auth.location.world", cfg.getString("spawn-world", "world"));
        ConfigurationSection section = cfg.getConfigurationSection("auth.location");
        double x = 0.5;
        double y = -21;
        double z = 0.5;
        float yaw = 0f;
        float pitch = 0f;
        if (section != null) {
            worldName = section.getString("world", worldName);
            x = section.getDouble("x", x);
            y = section.getDouble("y", y);
            z = section.getDouble("z", z);
            yaw = (float) section.getDouble("yaw", 0);
            pitch = (float) section.getDouble("pitch", 0);
        }
        World world = Bukkit.getWorld(worldName);
        authLocation = world == null
                ? new Location(null, x, y, z, yaw, pitch)
                : new Location(world, x, y, z, yaw, pitch);
    }

    public void refreshAuthLocation() {
        reload();
        if (authLocation.getWorld() != null) {
            return;
        }
        String name = plugin.getConfig().getString("auth.location.world",
                plugin.getConfig().getString("spawn-world", "world"));
        World loaded = Bukkit.getWorld(name);
        if (loaded != null) {
            Location loc = authLocation.clone();
            loc.setWorld(loaded);
            authLocation = loc;
        }
    }

    public Location authLocation() {
        return authLocation.clone();
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("auth.enabled", true);
    }

    /** Только явный bypass или auth.enabled=false. OP/админка НЕ отключают /reg. */
    public boolean bypass(Player player) {
        return !enabled() || player.hasPermission("pveauction.auth.bypass");
    }

    public boolean isLoggedIn(Player player) {
        return bypass(player) || loggedIn.contains(player.getUniqueId());
    }

    public boolean isAuthCommand(String label) {
        String cmd = label.toLowerCase(Locale.ROOT);
        return cmd.equals("register") || cmd.equals("reg")
                || cmd.equals("login") || cmd.equals("l");
    }

    public static String clientIp(Player player) {
        if (player.getAddress() == null || player.getAddress().getAddress() == null) {
            return "unknown";
        }
        return player.getAddress().getAddress().getHostAddress();
    }

    public void markLoggedIn(UUID uuid) {
        loggedIn.add(uuid);
        stopAuthTitleRefresh(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.clearTitle();
        }
    }

    public void markLoggedOut(UUID uuid) {
        loggedIn.remove(uuid);
        stopAuthTitleRefresh(uuid);
    }

    public void allowPluginTeleport(UUID uuid) {
        allowTeleport.add(uuid);
    }

    public boolean isJoinTeleportAllowed(UUID uuid) {
        return allowTeleport.contains(uuid);
    }

    public void handleJoin(Player player) {
        if (bypass(player)) {
            markLoggedIn(player.getUniqueId());
            scheduleWorldJoin(player, 5L);
            return;
        }

        String ip = clientIp(player);
        UUID uuid = player.getUniqueId();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                boolean registered = repository.isRegistered(uuid);
                boolean sessionOk = false;
                if (registered) {
                    sessionOk = repository.findSessionIp(uuid)
                            .map(savedIp -> savedIp.equals(ip))
                            .orElse(false);
                }

                boolean finalSessionOk = sessionOk;
                boolean finalRegistered = registered;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (finalSessionOk) {
                        markLoggedIn(uuid);
                        sendHint(player, finalRegistered, true);
                        scheduleWorldJoin(player, 5L);
                        return;
                    }
                    markLoggedOut(uuid);
                    sendHint(player, finalRegistered, false);
                    scheduleAuthTeleport(player, 2L);
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Auth join " + player.getName() + ": " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    markLoggedOut(uuid);
                    TravelMsg.send(player, TravelMsg.err("Ошибка авторизации. Попробуй /reg или /login."));
                    scheduleAuthTeleport(player, 2L);
                });
            }
        });
    }

    private void sendHint(Player player, boolean registered, boolean autoLogin) {
        if (autoLogin) {
            return;
        }
        Component subtitle = registered
                ? Component.text("/login <пароль>  или  /l", NamedTextColor.YELLOW)
                : Component.text("/reg <пароль>", NamedTextColor.YELLOW);
        player.sendMessage(subtitle);
        startAuthTitleRefresh(player, registered, subtitle);
    }

    private void showAuthTitle(Player player, Component subtitle) {
        player.showTitle(Title.title(
                Component.text("Нужна авторизация", NamedTextColor.GOLD),
                subtitle,
                Title.Times.times(
                        Duration.ofMillis(200),
                        Duration.ofSeconds(30),
                        Duration.ofMillis(200)
                )
        ));
    }

    /** Title не гаснет, пока игрок не вошёл. */
    private void startAuthTitleRefresh(Player player, boolean registered, Component subtitle) {
        UUID uuid = player.getUniqueId();
        stopAuthTitleRefresh(uuid);
        showAuthTitle(player, subtitle);
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || isLoggedIn(player)) {
                    stopAuthTitleRefresh(uuid);
                    if (player.isOnline()) {
                        player.clearTitle();
                    }
                    cancel();
                    return;
                }
                showAuthTitle(player, subtitle);
            }
        }.runTaskTimer(plugin, 100L, 100L);
        authTitleRefresh.put(uuid, task);
    }

    private void stopAuthTitleRefresh(UUID uuid) {
        BukkitTask task = authTitleRefresh.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    private void scheduleAuthTeleport(Player player, long delayTicks) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || isLoggedIn(player)) {
                    return;
                }
                refreshAuthLocation();
                Location target = authLocation.clone();
                if (target.getWorld() == null) {
                    player.sendMessage(TravelMsg.err("Мир авторизации не загружен. Сообщи админу."));
                    return;
                }
                allowPluginTeleport(player.getUniqueId());
                player.teleport(target);
            }
        }.runTaskLater(plugin, delayTicks);
    }

    public void scheduleWorldJoin(Player player, long delayTicks) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !isLoggedIn(player)) {
                    return;
                }
                allowPluginTeleport(player.getUniqueId());
                plugin.worlds().refreshLocations();
                JoinTeleportService.teleportOnJoin(plugin, player);
                plugin.scoreboardListener().refresh(player);
                plugin.getServer().getScheduler().runTaskLater(
                        plugin,
                        () -> allowTeleport.remove(player.getUniqueId()),
                        120L
                );
            }
        }.runTaskLater(plugin, delayTicks);
    }

    public void completeLogin(Player player, char[] password, boolean registering) {
        if (bypass(player)) {
            TravelMsg.send(player, TravelMsg.ok("Обход авторизации."));
            return;
        }
        if (isLoggedIn(player)) {
            TravelMsg.send(player, TravelMsg.ok("Ты уже вошёл."));
            return;
        }

        int minLen = plugin.getConfig().getInt("auth.min-password-length", 4);
        int maxLen = plugin.getConfig().getInt("auth.max-password-length", 32);
        if (password.length < minLen || password.length > maxLen) {
            TravelMsg.send(player, TravelMsg.err("Пароль: от " + minLen + " до " + maxLen + " символов."));
            return;
        }

        UUID uuid = player.getUniqueId();
        String ip = clientIp(player);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (registering) {
                    handleRegisterAsync(player, uuid, ip, password);
                } else {
                    handleLoginAsync(player, uuid, ip, password);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Auth: " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        TravelMsg.send(player, TravelMsg.err("Ошибка базы данных.")));
            }
        });
    }

    private void handleRegisterAsync(Player player, UUID uuid, String ip, char[] password) throws SQLException {
        if (repository.isRegistered(uuid)) {
            notify(player, TravelMsg.err("Аккаунт уже есть. /login <пароль>"));
            return;
        }

        String hash = PasswordHasher.hash(password);
        repository.register(uuid, hash);
        repository.upsertSession(uuid, ip);
        finishAuth(player, uuid, TravelMsg.ok("Регистрация успешна. Добро пожаловать!"));
    }

    private void handleLoginAsync(Player player, UUID uuid, String ip, char[] password) throws SQLException {
        if (!repository.isRegistered(uuid)) {
            notify(player, TravelMsg.err("Сначала /reg <пароль>"));
            return;
        }
        String hash = repository.findPasswordHash(uuid).orElse(null);
        if (hash == null || !PasswordHasher.verify(password, hash)) {
            notify(player, TravelMsg.err("Неверный пароль."));
            return;
        }
        repository.upsertSession(uuid, ip);
        finishAuth(player, uuid, TravelMsg.ok("Вход выполнен."));
    }

    private void finishAuth(Player player, UUID uuid, Component message) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            markLoggedIn(uuid);
            TravelMsg.send(player, message);
            scheduleWorldJoin(player, 5L);
        });
    }

    private void notify(Player player, Component message) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                TravelMsg.send(player, message);
            }
        });
    }

    public AuthRepository repository() {
        return repository;
    }
}
