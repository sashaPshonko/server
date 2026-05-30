package dev.narek.pveauction.listener;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.item.CustomItems;
import dev.narek.pveauction.world.RtpTeleportHelper;
import dev.narek.pveauction.world.TravelEffects;
import dev.narek.pveauction.world.WorldTravelService;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
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

    /** Телепорт на спавн/сохранённую точку — только после авторизации (см. AuthService). */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // JoinTeleportService вызывается из AuthService после /login|/reg или сессии по IP.
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

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (plugin.auth() != null && !plugin.auth().isLoggedIn(event.getPlayer())) {
            return;
        }
        var spawn = worlds.spawnLocation();
        if (spawn.getWorld() != null) {
            event.setRespawnLocation(spawn);
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> applyWorldRules(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (plugin.auth() != null && !plugin.auth().isLoggedIn(event.getPlayer())) {
            return;
        }
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
        TravelEffects.applyRtpSlowFalling(plugin, player);
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

        if (plugin.auth() != null && !plugin.auth().isLoggedIn(player)) {
            return;
        }

        if (!plugin.getConfig().getBoolean("save-logout-location", true)) {
            return;
        }

        var location = player.getLocation().clone();
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.players().getOrCreate(uuid, name);
                plugin.players().saveLogoutLocation(uuid, location);
            } catch (Exception e) {
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
        if (CustomItems.keyType(plugin, event.getItemInHand()) != null) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEndermiteSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Endermite)) {
            return;
        }
        if (!worlds.isSpawnWorld(event.getLocation().getWorld())) {
            return;
        }
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.ENDER_PEARL) {
            event.setCancelled(true);
        }
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
        return player.hasPermission("pveauction.spawn.bypass")
                || player.hasPermission("pveauction.admin");
    }

    private void applyWorldRules(Player player) {
        if (worlds.isSpawnWorld(player.getWorld())) {
            worlds.applySpawnRules(player);
        } else if (worlds.isRtpWorld(player.getWorld())) {
            worlds.applyRtpRules(player);
        }
    }
}
