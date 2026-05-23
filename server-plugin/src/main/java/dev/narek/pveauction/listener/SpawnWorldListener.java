package dev.narek.pveauction.listener;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.model.SavedLocation;
import dev.narek.pveauction.world.RtpTeleportHelper;
import dev.narek.pveauction.world.WorldTeleportService;
import dev.narek.pveauction.world.WorldTravelService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class SpawnWorldListener implements Listener {

    private final PveAuctionPlugin plugin;
    private final WorldTravelService worlds;
    private final Set<UUID> rtpCooldown = new HashSet<>();

    public SpawnWorldListener(PveAuctionPlugin plugin) {
        this.plugin = plugin;
        this.worlds = plugin.worlds();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                worlds.refreshLocations();
                teleportOnJoin(player);
            }
        }.runTaskLater(plugin, 5L);
    }

    private void teleportOnJoin(Player player) {
        boolean saveLogout = plugin.getConfig().getBoolean("save-logout-location", true);

        if (saveLogout) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    plugin.players().getOrCreate(player.getUniqueId(), player.getName());
                    Optional<SavedLocation> saved = plugin.players().findLogoutLocation(player.getUniqueId());
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (!player.isOnline()) {
                            return;
                        }
                        if (saved.isPresent()) {
                            Location target = saved.get().toLocation();
                            if (target != null) {
                                WorldTeleportService.teleport(plugin, player, target, ok -> applyWorldRules(player));
                                return;
                            }
                        }
                        teleportFirstJoin(player);
                    });
                } catch (SQLException e) {
                    plugin.getLogger().severe("Позиция при входе: " + e.getMessage());
                    plugin.getServer().getScheduler().runTask(plugin, () -> teleportFirstJoin(player));
                }
            });
            return;
        }

        teleportFirstJoin(player);
    }

    private void teleportFirstJoin(Player player) {
        if (plugin.getConfig().getBoolean("teleport-to-spawn-on-join", true)) {
            WorldTeleportService.teleport(plugin, player, worlds.spawnLocation(), ok -> applyWorldRules(player));
        } else {
            applyWorldRules(player);
        }
    }

    private void applyWorldRules(Player player) {
        if (worlds.isSpawnWorld(player.getWorld())) {
            worlds.applySpawnRules(player);
        } else if (worlds.isRtpWorld(player.getWorld())) {
            worlds.applyRtpRules(player);
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                applyWorldRules(player);
            }
        }.runTask(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedPosition()) {
            return;
        }
        Player player = event.getPlayer();
        if (!worlds.shouldAutoRtpOnSpawn(event.getFrom(), event.getTo())) {
            return;
        }
        if (rtpCooldown.contains(player.getUniqueId())) {
            return;
        }
        rtpCooldown.add(player.getUniqueId());
        RtpTeleportHelper.teleportRandom(plugin, worlds, player);
        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> rtpCooldown.remove(player.getUniqueId()),
                plugin.getConfig().getLong("spawn-rtp-cooldown-ticks", 40L)
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        rtpCooldown.remove(player.getUniqueId());

        if (!plugin.getConfig().getBoolean("save-logout-location", true)) {
            return;
        }

        Location location = player.getLocation().clone();
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.players().getOrCreate(uuid, name);
                plugin.players().saveLogoutLocation(uuid, location);
            } catch (SQLException e) {
                plugin.getLogger().severe("Сохранение позиции: " + e.getMessage());
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!worlds.isSpawnWorld(event.getBlock().getWorld())) {
            return;
        }
        if (canBypass(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!worlds.isSpawnWorld(event.getBlock().getWorld())) {
            return;
        }
        if (canBypass(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!worlds.isSpawnWorld(player.getWorld())) {
            return;
        }
        if (canBypass(player)) {
            return;
        }
        event.setCancelled(true);
    }

    private boolean canBypass(Player player) {
        return player.hasPermission("pveauction.spawn.bypass");
    }
}
