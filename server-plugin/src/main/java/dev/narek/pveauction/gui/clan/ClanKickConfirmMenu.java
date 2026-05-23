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

import java.util.UUID;

public final class ClanKickConfirmMenu implements InventoryHolder {

    public static final int SLOT_CONFIRM = 11;
    public static final int SLOT_CANCEL = 15;

    private final Player viewer;
    private final String targetName;
    private final UUID targetUuid;
    private Inventory inventory;

    private ClanKickConfirmMenu(Player viewer, String targetName, UUID targetUuid) {
        this.viewer = viewer;
        this.targetName = targetName;
        this.targetUuid = targetUuid;
    }

    public static void open(dev.narek.pveauction.PveAuctionPlugin plugin, Player viewer, String targetName, UUID targetUuid) {
        ClanKickConfirmMenu menu = new ClanKickConfirmMenu(viewer, targetName, targetUuid);
        menu.inventory = Bukkit.createInventory(menu, 27,
                GuiText.title("Кик: " + targetName, NamedTextColor.RED));
        menu.fill();
        viewer.openInventory(menu.inventory);
    }

    private void fill() {
        ItemStack glass = GuiItems.glassFill();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, glass);
        }
        inventory.setItem(SLOT_CONFIRM, GuiItems.button(Material.LIME_WOOL,
                Component.text("ДА, ИСКЛЮЧИТЬ " + targetName, NamedTextColor.GREEN, TextDecoration.BOLD)));
        inventory.setItem(SLOT_CANCEL, GuiItems.button(Material.RED_WOOL,
                Component.text("ОТМЕНА", NamedTextColor.RED, TextDecoration.BOLD)));
    }

    public Player viewer() {
        return viewer;
    }

    public String targetName() {
        return targetName;
    }

    public UUID targetUuid() {
        return targetUuid;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
