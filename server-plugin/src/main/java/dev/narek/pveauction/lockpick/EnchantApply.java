package dev.narek.pveauction.lockpick;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Зачарования выше ванильного максимума (Защита V и т.д.). */
public final class EnchantApply {

    private EnchantApply() {}

    public static void set(ItemStack item, Enchantment enchantment, int level) {
        if (item == null || enchantment == null || level < 1) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.addEnchant(enchantment, level, true);
        item.setItemMeta(meta);
    }
}
