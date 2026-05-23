package dev.narek.pveauction.gui.clan;

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
import org.bukkit.inventory.ItemStack;

public final class ClanActionConfirmMenu implements InventoryHolder {

    public enum Action {
        LEAVE("Покинуть клан?", NamedTextColor.YELLOW, "ПОКИНУТЬ", Material.LIME_WOOL),
        DISBAND("Расформировать клан?", NamedTextColor.RED, "РАСФОРМИРОВАТЬ", Material.TNT),
        DEL_HOME("Удалить клановый дом?", NamedTextColor.GOLD, "УДАЛИТЬ ДОМ", Material.BARRIER);

        private final String title;
        private final NamedTextColor color;
        private final String confirmLabel;
        private final Material confirmMaterial;

        Action(String title, NamedTextColor color, String confirmLabel, Material confirmMaterial) {
            this.title = title;
            this.color = color;
            this.confirmLabel = confirmLabel;
            this.confirmMaterial = confirmMaterial;
        }
    }

    public static final int SLOT_CONFIRM = 11;
    public static final int SLOT_CANCEL = 15;

    private final Player viewer;
    private final Action action;
    private Inventory inventory;

    private ClanActionConfirmMenu(Player viewer, Action action) {
        this.viewer = viewer;
        this.action = action;
    }

    public static void open(Player viewer, Action action) {
        ClanActionConfirmMenu menu = new ClanActionConfirmMenu(viewer, action);
        menu.inventory = Bukkit.createInventory(menu, 27,
                GuiText.title(action.title, action.color));
        menu.fill();
        viewer.openInventory(menu.inventory);
    }

    private void fill() {
        ItemStack glass = GuiItems.glassFill();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, glass);
        }
        inventory.setItem(13, GuiItems.button(Material.PAPER,
                Component.text(action.title, NamedTextColor.WHITE, TextDecoration.BOLD)));
        inventory.setItem(SLOT_CONFIRM, GuiItems.button(action.confirmMaterial,
                Component.text("ДА, " + action.confirmLabel, NamedTextColor.GREEN, TextDecoration.BOLD)));
        inventory.setItem(SLOT_CANCEL, GuiItems.button(Material.RED_WOOL,
                Component.text("ОТМЕНА", NamedTextColor.RED, TextDecoration.BOLD)));
    }

    public Player viewer() {
        return viewer;
    }

    public Action action() {
        return action;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
