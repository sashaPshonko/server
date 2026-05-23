package dev.narek.pveauction.listener;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.world.RtpTeleportHelper;
import dev.narek.pveauction.world.WorldTeleportService;
import dev.narek.pveauction.world.WorldTravelService;
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

import java.util.HashSet;
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
                if (plugin.getConfig().getBoolean("teleport-to-spawn-on-join", true)) {
                    WorldTeleportService.teleport(plugin, player, worlds.spawnLocation(), ok -> {
                        if (ok) {
                            worlds.applySpawnRules(player);
                        }
                    });
                } else if (worlds.isSpawnWorld(player.getWorld())) {
                    worlds.applySpawnRules(player);
                }
            }
        }.runTaskLater(plugin, 5L);
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
                if (worlds.isSpawnWorld(player.getWorld())) {
                    worlds.applySpawnRules(player);
                } else if (worlds.isRtpWorld(player.getWorld())) {
                    worlds.applyRtpRules(player);
                }
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

    @EventHandler(ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent event) {
        rtpCooldown.remove(event.getPlayer().getUniqueId());
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
