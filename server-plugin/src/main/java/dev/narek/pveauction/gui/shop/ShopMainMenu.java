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

    public static final int SLOT_BUY = 11;
    public static final int SLOT_SELL = 15;

    private final Player viewer;
    private Inventory inventory;

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
        inventory.setItem(4, GuiItems.button(Material.NETHER_STAR,
                Component.text("4NAREK", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text("Торговый центр", NamedTextColor.GRAY)));
        inventory.setItem(SLOT_BUY, GuiItems.button(Material.EMERALD,
                Component.text("МАГАЗИН", NamedTextColor.GRAY, TextDecoration.BOLD),
                Component.text("Скоро", NamedTextColor.DARK_GRAY)));
        inventory.setItem(SLOT_SELL, GuiItems.button(Material.GOLD_INGOT,
                Component.text("СКУПКА", NamedTextColor.GREEN, TextDecoration.BOLD),
                Component.text("ЛКМ — открыть", NamedTextColor.YELLOW)));
    }

    public Player viewer() {
        return viewer;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
