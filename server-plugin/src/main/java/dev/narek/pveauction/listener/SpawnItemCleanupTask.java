package dev.narek.pveauction.listener;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.world.WorldTravelService;
import org.bukkit.World;
import org.bukkit.entity.Item;

/** Удаляет дроп на спавне, если лежит дольше заданного времени. */
public final class SpawnItemCleanupTask implements Runnable {

    private final PveAuctionPlugin plugin;
    private final WorldTravelService worlds;
    private final int maxTicks;

    public SpawnItemCleanupTask(PveAuctionPlugin plugin, int despawnSeconds) {
        this.plugin = plugin;
        this.worlds = plugin.worlds();
        this.maxTicks = Math.max(20, despawnSeconds * 20);
    }

    @Override
    public void run() {
        for (World world : plugin.getServer().getWorlds()) {
            if (!worlds.isSpawnWorld(world)) {
                continue;
            }
            for (Item item : world.getEntitiesByClass(Item.class)) {
                if (item.getTicksLived() >= maxTicks) {
                    item.remove();
                }
            }
        }
    }
}
