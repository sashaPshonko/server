package dev.narek.pveauction.gui.shop;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.shop.ShopCategory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public final class ShopGuiTags {

    private ShopGuiTags() {}

    public static ItemStack tagCategory(PveAuctionPlugin plugin, ItemStack item, ShopCategory category) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(categoryKey(plugin), PersistentDataType.STRING, category.id());
            item.setItemMeta(meta);
        }
        return item;
    }

    public static Optional<ShopCategory> readCategory(PveAuctionPlugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }
        String id = item.getItemMeta().getPersistentDataContainer().get(categoryKey(plugin), PersistentDataType.STRING);
        if (id == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(ShopCategory.byId(id));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static ItemStack tagMaterial(PveAuctionPlugin plugin, ItemStack item, Material material) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(materialKey(plugin), PersistentDataType.STRING, material.name());
            item.setItemMeta(meta);
        }
        return item;
    }

    public static Optional<Material> readMaterial(PveAuctionPlugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }
        String name = item.getItemMeta().getPersistentDataContainer().get(materialKey(plugin), PersistentDataType.STRING);
        if (name == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Material.valueOf(name));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static NamespacedKey categoryKey(PveAuctionPlugin plugin) {
        return new NamespacedKey(plugin, "shop_category");
    }

    private static NamespacedKey materialKey(PveAuctionPlugin plugin) {
        return new NamespacedKey(plugin, "shop_material");
    }
}
