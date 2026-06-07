package dev.narek.pveauction.lockpick.armor;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.item.CustomItems;
import dev.narek.pveauction.item.StorageKeyType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/** Проверка: отмычка к броне + кузнечный стол на спавне. */
public final class ArmorLockpickInteract {

    private ArmorLockpickInteract() {}

    public static boolean isArmorKey(ItemStack stack, PveAuctionPlugin plugin) {
        return CustomItems.keyType(plugin, stack) == StorageKeyType.ARMOR;
    }

    public static ItemStack armorKeyInMainHand(Player player) {
        return player.getInventory().getItemInMainHand();
    }

    public static boolean holdingArmorKey(Player player, PveAuctionPlugin plugin) {
        return isArmorKey(armorKeyInMainHand(player), plugin);
    }

    /**
     * Кузнечный стол: клик по блоку или прицел (ПКМ иногда без clickedBlock).
     */
    public static Block resolveSmithingBlock(PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        if (clicked != null && clicked.getType() == Material.SMITHING_TABLE) {
            return clicked;
        }
        Player player = event.getPlayer();
        Block target = player.getTargetBlockExact(8);
        if (target != null && target.getType() == Material.SMITHING_TABLE) {
            return target;
        }
        var ray = player.rayTraceBlocks(8);
        if (ray != null && ray.getHitBlock() != null
                && ray.getHitBlock().getType() == Material.SMITHING_TABLE) {
            return ray.getHitBlock();
        }
        return null;
    }

    public static boolean isBlockClick(PlayerInteractEvent event) {
        Action action = event.getAction();
        return action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK;
    }

    public static boolean isSmithingTableAttempt(
            PlayerInteractEvent event,
            PveAuctionPlugin plugin,
            ArmorLockpickService lockpick
    ) {
        if (!holdingArmorKey(event.getPlayer(), plugin)) {
            return false;
        }
        Action action = event.getAction();
        boolean useBlock = isBlockClick(event);
        boolean useAir = action == Action.RIGHT_CLICK_AIR || action == Action.LEFT_CLICK_AIR;
        if (!useBlock && !useAir) {
            return false;
        }
        Block block = resolveSmithingBlock(event);
        return lockpick.isSpawnSmithing(block);
    }

    public static Block smithingBlockFromOpen(InventoryOpenEvent event, Player player) {
        if (event.getInventory().getType() != InventoryType.SMITHING) {
            return null;
        }
        var holderLoc = event.getInventory().getLocation();
        if (holderLoc != null) {
            Block block = holderLoc.getBlock();
            if (block.getType() == Material.SMITHING_TABLE) {
                return block;
            }
        }
        Block target = player.getTargetBlockExact(6);
        if (target != null && target.getType() == Material.SMITHING_TABLE) {
            return target;
        }
        return null;
    }

    public static boolean isSmithingTableOpen(
            InventoryOpenEvent event,
            PveAuctionPlugin plugin,
            ArmorLockpickService lockpick,
            Player player
    ) {
        if (!holdingArmorKey(player, plugin)) {
            return false;
        }
        Block block = smithingBlockFromOpen(event, player);
        return block != null && lockpick.isSpawnSmithing(block);
    }
}
