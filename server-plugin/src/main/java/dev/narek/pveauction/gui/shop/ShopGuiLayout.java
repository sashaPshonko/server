package dev.narek.pveauction.gui.shop;

import dev.narek.pveauction.util.GuiItems;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class ShopGuiLayout {

    private ShopGuiLayout() {}

    public static void fillChest54(Inventory inventory) {
        ItemStack black = GuiItems.decorPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack gray = GuiItems.decorPane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, gray);
        }
        for (int col = 0; col < 9; col++) {
            inventory.setItem(col, black);
            inventory.setItem(45 + col, black);
        }
        for (int row = 1; row <= 4; row++) {
            inventory.setItem(row * 9, black);
            inventory.setItem(row * 9 + 8, black);
        }
    }

    /** Скупка: только верх/низ чёрные, без боковых рамок — 9 колонок под товары. */
    public static void fillChest54Sell(Inventory inventory) {
        ItemStack black = GuiItems.decorPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack gray = GuiItems.decorPane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, gray);
        }
        for (int col = 0; col < 9; col++) {
            inventory.setItem(col, black);
            inventory.setItem(45 + col, black);
        }
    }

    public static void fillChest27(Inventory inventory) {
        ItemStack black = GuiItems.decorPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack gray = GuiItems.decorPane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, gray);
        }
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, black);
            inventory.setItem(18 + i, black);
        }
        inventory.setItem(9, black);
        inventory.setItem(17, black);
    }
}
