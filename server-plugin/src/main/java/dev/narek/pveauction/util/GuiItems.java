package dev.narek.pveauction.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class GuiItems {

    private GuiItems() {}

    public static ItemStack button(Material mat, Component name, Component... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            if (lore.length > 0) {
                meta.lore(List.of(lore));
            }
            decorateMeta(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack glassFill() {
        return decorPane(Material.GRAY_STAINED_GLASS_PANE, " ");
    }

    public static ItemStack decorPane(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            decorateMeta(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void decorateMeta(ItemMeta meta) {
        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ADDITIONAL_TOOLTIP,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE
        );
    }

    public static String formatPrice(long price) {
        return String.format("%,d", price).replace(',', ' ');
    }

    public static List<Component> lotLore(
            long price,
            String sellerName,
            boolean own,
            boolean buyHint,
            long createdAt,
            long expiryMs
    ) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Цена: ", NamedTextColor.GRAY)
                .append(Component.text(formatPrice(price) + " $", NamedTextColor.GOLD, TextDecoration.BOLD)));
        lore.add(Component.text("Продавец: ", NamedTextColor.GRAY)
                .append(Component.text(sellerName, NamedTextColor.AQUA)));

        long expiresAt = LotExpiry.expiresAt(createdAt, expiryMs);
        long now = System.currentTimeMillis();
        if (now >= expiresAt) {
            lore.add(Component.text("Статус: ", NamedTextColor.GRAY)
                    .append(Component.text("истёк", NamedTextColor.RED, TextDecoration.BOLD)));
            if (own) {
                lore.add(Component.text("Перевыстави в хранилище", NamedTextColor.GOLD));
            }
        } else {
            long left = expiresAt - now;
            lore.add(Component.text("Истекает через: ", NamedTextColor.GRAY)
                    .append(Component.text(LotExpiry.formatRemaining(left), NamedTextColor.YELLOW)));
        }

        if (own) {
            lore.add(Component.text("ЛКМ — снять с аукциона", NamedTextColor.LIGHT_PURPLE));
        } else if (buyHint) {
            lore.add(Component.text("ЛКМ — купить", NamedTextColor.GREEN, TextDecoration.BOLD));
        }
        return lore;
    }
}
