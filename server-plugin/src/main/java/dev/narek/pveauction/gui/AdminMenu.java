package dev.narek.pveauction.gui;

import dev.narek.pveauction.util.GuiItems;
import dev.narek.pveauction.util.GuiText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class AdminMenu implements InventoryHolder {

    public static final int SIZE = 27;
    public static final int SLOT_CREATIVE = 11;
    public static final int SLOT_GIVE_10K = 13;
    public static final int SLOT_GIVE_100K = 15;
    public static final int SLOT_INFO = 22;

    private final Player viewer;
    private Inventory inventory;

    private AdminMenu(Player viewer) {
        this.viewer = viewer;
    }

    public static void open(Player player) {
        AdminMenu menu = new AdminMenu(player);
        menu.inventory = Bukkit.createInventory(menu, SIZE, GuiText.TITLE_ADMIN);
        menu.fill();
        player.openInventory(menu.inventory);
    }

    private void fill() {
        inventory.clear();
        boolean creative = viewer.getGameMode() == GameMode.CREATIVE;
        inventory.setItem(SLOT_CREATIVE, GuiItems.button(
                creative ? Material.LIME_DYE : Material.GRAY_DYE,
                Component.text("КРЕАТИВ: " + (creative ? "ВКЛ" : "ВЫКЛ"),
                        creative ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD),
                Component.text("Переключить режим", NamedTextColor.GRAY)));
        inventory.setItem(SLOT_GIVE_10K, GuiItems.button(Material.GOLD_INGOT,
                Component.text("ВЫДАТЬ 10 000 $", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text("На свой счёт", NamedTextColor.AQUA)));
        inventory.setItem(SLOT_GIVE_100K, GuiItems.button(Material.GOLD_BLOCK,
                Component.text("ВЫДАТЬ 100 000 $", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text("На свой счёт", NamedTextColor.AQUA)));
        inventory.setItem(SLOT_INFO, GuiItems.button(Material.BOOK,
                Component.text("ВЫДАТЬ ИГРОКУ", NamedTextColor.WHITE, TextDecoration.BOLD),
                Component.text("/admin give <ник> <сумма>", NamedTextColor.LIGHT_PURPLE)));
    }

    public void reload() {
        fill();
    }

    public Player viewer() {
        return viewer;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
