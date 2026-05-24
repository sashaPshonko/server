package dev.narek.pveauction.gui.shop;

import dev.narek.pveauction.util.GuiItems;
import dev.narek.pveauction.util.GuiText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class ShopMainMenu implements InventoryHolder {

    private static final int CHEST27_ROWS = 3;
    private static final int CHEST27_ROW_START = 1;

    private final Player viewer;
    private Inventory inventory;
    private int slotBuy;
    private int slotSell;

    private ShopMainMenu(Player viewer) {
        this.viewer = viewer;
    }

    public static void open(Player player) {
        ShopMainMenu menu = new ShopMainMenu(player);
        menu.inventory = Bukkit.createInventory(menu, 27, GuiText.title("Магазин", NamedTextColor.GOLD));
        menu.fill();
        player.openInventory(menu.inventory);
    }

    private void fill() {
        ShopGuiLayout.fillChest27(inventory);
        int[] slots = ShopGuiGridLayout.slotsForCount(2, 0, ShopGuiGridLayout.WIDTH, CHEST27_ROW_START, CHEST27_ROWS);
        slotBuy = slots[0];
        slotSell = slots[1];

        inventory.setItem(4, GuiItems.button(Material.NETHER_STAR,
                Component.text("4NAREK", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text("Торговый центр", NamedTextColor.GRAY)));
        inventory.setItem(slotBuy, GuiItems.button(Material.EMERALD,
                Component.text("МАГАЗИН", NamedTextColor.GRAY, TextDecoration.BOLD),
                Component.text("Скоро", NamedTextColor.DARK_GRAY)));
        inventory.setItem(slotSell, GuiItems.button(Material.GOLD_INGOT,
                Component.text("СКУПКА", NamedTextColor.GREEN, TextDecoration.BOLD),
                Component.text("ЛКМ — открыть", NamedTextColor.YELLOW)));
    }

    public int slotSell() {
        return slotSell;
    }

    public Player viewer() {
        return viewer;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
