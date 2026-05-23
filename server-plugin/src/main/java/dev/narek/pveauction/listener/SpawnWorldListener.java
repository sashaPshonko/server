package dev.narek.pveauction.listener;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.world.RtpTeleportHelper;
import dev.narek.pveauction.world.WorldTravelService;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
    private final Set<UUID> portalCooldown = new HashSet<>();

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
                    player.teleportAsync(worlds.spawnLocation()).thenAccept(ok -> {
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
        if (!worlds.isInSpawnPortal(event.getTo())) {
            return;
        }
        if (portalCooldown.contains(player.getUniqueId())) {
            return;
        }
        portalCooldown.add(player.getUniqueId());
        RtpTeleportHelper.teleportRandom(plugin, worlds, player, "Портал отправил в мир.");
        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> portalCooldown.remove(player.getUniqueId()),
                plugin.getConfig().getLong("spawn-portal-cooldown-ticks", 60L)
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent event) {
        portalCooldown.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!worlds.isSpawnWorld(event.getBlock().getWorld())) {
            return;
        }
        if (canBuild(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!worlds.isSpawnWorld(event.getBlock().getWorld())) {
            return;
        }
        if (canBuild(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (!worlds.isSpawnWorld(victim.getWorld())) {
            return;
        }
        Entity damager = event.getDamager();
        Player attacker = null;
        if (damager instanceof Player p) {
            attacker = p;
        } else if (damager instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof Player p) {
            attacker = p;
        }
        if (attacker == null) {
            return;
        }
        if (canBuild(attacker)) {
            return;
        }
        event.setCancelled(true);
    }

    private boolean canBuild(Player player) {
        return player.hasPermission("pveauction.spawn.bypass");
    }
}
