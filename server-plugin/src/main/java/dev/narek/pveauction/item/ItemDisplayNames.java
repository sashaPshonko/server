package dev.narek.pveauction.item;

import dev.narek.pveauction.PveAuctionPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Человекочитаемое имя предмета (серебро, отмычки, displayName, не «кусочек железа»). */
public final class ItemDisplayNames {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private ItemDisplayNames() {}

    public static String describe(PveAuctionPlugin plugin, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return "предмет";
        }
        if (CustomItems.isSilver(plugin, stack)) {
            int amount = stack.getAmount();
            return amount > 1 ? "Серебро ×" + amount : "Серебро";
        }
        StorageKeyType key = CustomItems.keyType(plugin, stack);
        if (key != null) {
            return plain(key.displayName());
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return plain(meta.displayName());
        }
        return plain(stack.displayName());
    }

    private static String plain(Component component) {
        if (component == null) {
            return "предмет";
        }
        String text = PLAIN.serialize(component).trim();
        return text.isEmpty() ? "предмет" : text;
    }
}
