package dev.narek.pveauction.listener;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.lockpick.armor.ArmorLockpickInteract;
import dev.narek.pveauction.lockpick.armor.ArmorLockpickService;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class ArmorLockpickListener implements Listener {

    private final PveAuctionPlugin plugin;
    private final ArmorLockpickService lockpick;

    public ArmorLockpickListener(PveAuctionPlugin plugin, ArmorLockpickService lockpick) {
        this.plugin = plugin;
        this.lockpick = lockpick;
    }

    public ArmorLockpickListener(PveAuctionPlugin plugin) {
        this(plugin, new ArmorLockpickService(plugin));
    }

    /**
     * MONITOR + следующий тик: срабатывает даже если HIGH отменил ивент (приваты, ключи).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onSmithingInteractMonitor(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!ArmorLockpickInteract.isSmithingTableAttempt(event, plugin, lockpick)) {
            return;
        }
        Block block = ArmorLockpickInteract.resolveSmithingBlock(event);
        if (block == null) {
            return;
        }
        denyBlockUse(event);
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (!ArmorLockpickInteract.holdingArmorKey(player, plugin)) {
                return;
            }
            if (!lockpick.isSpawnSmithing(block)) {
                return;
            }
            activate(player, block);
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onSmithingOpenLowest(InventoryOpenEvent event) {
        handleSmithingOpen(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSmithingOpenHighest(InventoryOpenEvent event) {
        handleSmithingOpen(event);
    }

    private void handleSmithingOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!ArmorLockpickInteract.isSmithingTableOpen(event, plugin, lockpick, player)) {
            return;
        }
        Block block = ArmorLockpickInteract.smithingBlockFromOpen(event, player);
        if (block == null) {
            return;
        }
        event.setCancelled(true);
        activate(player, block);
    }

    private static void denyBlockUse(PlayerInteractEvent event) {
        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
    }

    private void activate(Player player, Block block) {
        ArmorLockpickService.Result result = lockpick.tryActivate(player, block);
        if (result != ArmorLockpickService.Result.SUCCESS) {
            lockpick.sendFailure(player, result);
        }
    }
}
