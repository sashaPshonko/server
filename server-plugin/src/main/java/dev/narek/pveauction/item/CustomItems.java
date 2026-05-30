package dev.narek.pveauction.item;

import dev.narek.pveauction.PveAuctionPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import dev.narek.pveauction.util.GuiItems;

import java.util.List;

/** Серебро и отмычки к хранилищам (NBT / PDC). */
public final class CustomItems {

    public static final String PDC_SILVER = "silver";
    public static final String PDC_KEY_TYPE = "storage_key";

    private CustomItems() {}

    public static NamespacedKey silverKey(PveAuctionPlugin plugin) {
        return new NamespacedKey(plugin, PDC_SILVER);
    }

    public static NamespacedKey keyTypeKey(PveAuctionPlugin plugin) {
        return new NamespacedKey(plugin, PDC_KEY_TYPE);
    }

    public static ItemStack silver(PveAuctionPlugin plugin, int amount) {
        ItemStack item = new ItemStack(Material.IRON_NUGGET, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("✦ ", NamedTextColor.WHITE)
                    .append(Component.text("Серебро", NamedTextColor.GRAY, TextDecoration.BOLD)));
            meta.lore(List.of(
                    Component.empty(),
                    Component.text("Валюта торгаша", NamedTextColor.DARK_GRAY),
                    Component.text("Покупай отмычки у NPC на спавне", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("§8[§7серебро§8]", NamedTextColor.DARK_GRAY)
            ));
            meta.getPersistentDataContainer().set(silverKey(plugin), PersistentDataType.BYTE, (byte) 1);
            GuiItems.decorateMeta(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack storageKey(PveAuctionPlugin plugin, StorageKeyType type) {
        ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(type.displayName());
            meta.lore(type.lore());
            meta.getPersistentDataContainer().set(keyTypeKey(plugin), PersistentDataType.STRING, type.id());
            GuiItems.decorateMeta(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isSilver(PveAuctionPlugin plugin, ItemStack stack) {
        if (stack == null || stack.getType() != Material.IRON_NUGGET || !stack.hasItemMeta()) {
            return false;
        }
        return stack.getItemMeta().getPersistentDataContainer().has(silverKey(plugin), PersistentDataType.BYTE);
    }

    public static StorageKeyType keyType(PveAuctionPlugin plugin, ItemStack stack) {
        if (stack == null || stack.getType() != Material.TRIPWIRE_HOOK || !stack.hasItemMeta()) {
            return null;
        }
        String id = stack.getItemMeta().getPersistentDataContainer().get(keyTypeKey(plugin), PersistentDataType.STRING);
        return StorageKeyType.byId(id);
    }

    public static int countSilver(PveAuctionPlugin plugin, Player player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (isSilver(plugin, stack)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    public static boolean takeSilver(PveAuctionPlugin plugin, Player player, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (countSilver(plugin, player) < amount) {
            return false;
        }
        int left = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (!isSilver(plugin, stack)) {
                continue;
            }
            int take = Math.min(left, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) {
                contents[i] = null;
            }
            left -= take;
            if (left <= 0) {
                break;
            }
        }
        player.getInventory().setStorageContents(contents);
        return left == 0;
    }

    public static int addSilver(PveAuctionPlugin plugin, Player player, int amount) {
        int given = 0;
        int left = amount;
        while (left > 0) {
            int stackSize = Math.min(64, left);
            ItemStack stack = silver(plugin, stackSize);
            var leftover = player.getInventory().addItem(stack);
            if (!leftover.isEmpty()) {
                int notFit = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
                given += stackSize - notFit;
                return given;
            }
            given += stackSize;
            left -= stackSize;
        }
        return given;
    }
}
