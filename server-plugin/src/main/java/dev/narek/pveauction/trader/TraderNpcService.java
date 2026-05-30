package dev.narek.pveauction.trader;

import dev.narek.pveauction.PveAuctionPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/** Торгаш на спавне — житель, смотрит на ближайшего игрока. */
public final class TraderNpcService {

    public static final String PDC_TRADER = "trader_npc";

    private final PveAuctionPlugin plugin;
    private final NamespacedKey traderKey;
    private UUID npcUuid;
    private BukkitTask lookTask;

    public TraderNpcService(PveAuctionPlugin plugin) {
        this.plugin = plugin;
        this.traderKey = new NamespacedKey(plugin, PDC_TRADER);
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("trader.enabled", true)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, this::respawn, 40L);
    }

    public void stop() {
        if (lookTask != null) {
            lookTask.cancel();
            lookTask = null;
        }
        removeNpc();
    }

    public void respawn() {
        if (!plugin.getConfig().getBoolean("trader.enabled", true)) {
            return;
        }
        removeNpc();
        Location loc = traderLocation();
        if (loc == null || loc.getWorld() == null) {
            plugin.getLogger().warning("Торгаш: мир не найден — проверь trader.location в config.yml");
            return;
        }
        spawnVillager(loc);
        long interval = plugin.getConfig().getLong("trader.look-interval-ticks", 5L);
        lookTask = Bukkit.getScheduler().runTaskTimer(plugin, this::lookAtNearest, 20L, interval);
        plugin.getLogger().info("Торгаш (житель) заспавнен: "
                + loc.getWorld().getName() + " "
                + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
    }

    private void spawnVillager(Location loc) {
        Villager villager = loc.getWorld().spawn(loc, Villager.class, v -> {
            v.setAI(false);
            v.setSilent(true);
            v.setInvulnerable(true);
            v.setCollidable(true);
            v.setPersistent(true);
            v.setRemoveWhenFarAway(false);
            v.setGravity(false);
            v.setCustomNameVisible(true);
            v.customName(Component.text("Торгаш", NamedTextColor.GOLD, TextDecoration.BOLD));
            v.setProfession(Villager.Profession.NITWIT);
            v.setVillagerType(parseVillagerType(plugin.getConfig().getString("trader.villager-type", "PLAINS")));
            v.setAgeLock(true);
            v.setAdult();
            v.getPersistentDataContainer().set(traderKey, PersistentDataType.BYTE, (byte) 1);
        });
        npcUuid = villager.getUniqueId();
    }

    private static Villager.Type parseVillagerType(String raw) {
        if (raw == null || raw.isBlank()) {
            return Villager.Type.PLAINS;
        }
        try {
            return Villager.Type.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Villager.Type.PLAINS;
        }
    }

    public boolean isTrader(Entity entity) {
        return entity != null && entity.getPersistentDataContainer().has(traderKey, PersistentDataType.BYTE);
    }

    public Location traderLocation() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("trader.location");
        String worldName = plugin.getConfig().getString("spawn-world", "world");
        double x = -4.5;
        double y = -21;
        double z = -7.5;
        float yaw = 0;
        if (sec != null) {
            worldName = sec.getString("world", worldName);
            x = sec.getDouble("x", x);
            y = sec.getDouble("y", y);
            z = sec.getDouble("z", z);
            yaw = (float) sec.getDouble("yaw", 0);
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, 0);
    }

    private void lookAtNearest() {
        Entity entity = npcUuid == null ? null : Bukkit.getEntity(npcUuid);
        if (!(entity instanceof LivingEntity mob) || !mob.isValid()) {
            if (lookTask != null) {
                lookTask.cancel();
                lookTask = null;
            }
            Bukkit.getScheduler().runTask(plugin, this::respawn);
            return;
        }
        Location loc = mob.getLocation();
        World world = loc.getWorld();
        if (world == null) {
            return;
        }
        double range = plugin.getConfig().getDouble("trader.look-range", 16.0);
        double rangeSq = range * range;
        Player nearest = null;
        double best = rangeSq;
        for (Player p : world.getPlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR || !p.isOnline()) {
                continue;
            }
            double d = p.getLocation().distanceSquared(loc);
            if (d < best) {
                best = d;
                nearest = p;
            }
        }
        if (nearest == null) {
            return;
        }
        Location eye = mob.getEyeLocation();
        Vector to = nearest.getEyeLocation().toVector().subtract(eye.toVector());
        if (to.lengthSquared() < 0.01) {
            return;
        }
        eye.setDirection(to);
        mob.setRotation(eye.getYaw(), eye.getPitch());
    }

    private void removeNpc() {
        if (lookTask != null) {
            lookTask.cancel();
            lookTask = null;
        }
        if (npcUuid != null) {
            Entity e = Bukkit.getEntity(npcUuid);
            if (e != null) {
                e.remove();
            }
            npcUuid = null;
        }
        Location loc = traderLocation();
        if (loc == null || loc.getWorld() == null) {
            return;
        }
        for (Entity nearby : loc.getWorld().getNearbyEntities(loc, 2, 2, 2)) {
            if (isTrader(nearby)) {
                nearby.remove();
            }
        }
    }
}
