package dev.narek.pveauction.gui.clan;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.clan.ClanService;
import dev.narek.pveauction.model.ClanMember;
import dev.narek.pveauction.model.ClanPermissions;
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

public final class ClanMemberPermMenu implements InventoryHolder {

    public static final int SLOT_BACK = 45;
    public static final int SLOT_INVITE = 20;
    public static final int SLOT_KICK = 22;
    public static final int SLOT_WITHDRAW = 24;

    private final PveAuctionPlugin plugin;
    private final Player viewer;
    private final ClanMember target;
    private int permissions;
    private Inventory inventory;

    private ClanMemberPermMenu(PveAuctionPlugin plugin, Player viewer, ClanMember target) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.target = target;
        this.permissions = target.permissions();
    }

    public static void open(PveAuctionPlugin plugin, Player viewer, ClanMember target) {
        ClanMemberPermMenu menu = new ClanMemberPermMenu(plugin, viewer, target);
        menu.inventory = Bukkit.createInventory(menu, 54,
                GuiText.title("Права: " + target.playerName(), NamedTextColor.LIGHT_PURPLE));
        menu.fill();
        viewer.openInventory(menu.inventory);
    }

    private void fill() {
        inventory.clear();
        ItemStack glass = GuiItems.glassFill();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, glass);
        }
        inventory.setItem(SLOT_INVITE, toggle("Приглашать", ClanPermissions.INVITE));
        inventory.setItem(SLOT_KICK, toggle("Кикать", ClanPermissions.KICK));
        inventory.setItem(SLOT_WITHDRAW, toggle("Снимать деньги", ClanPermissions.WITHDRAW));
        inventory.setItem(SLOT_BACK, GuiItems.button(Material.ARROW,
                Component.text("НАЗАД", NamedTextColor.GREEN, TextDecoration.BOLD)));
    }

    private org.bukkit.inventory.ItemStack toggle(String label, int flag) {
        boolean on = ClanPermissions.has(permissions, flag);
        Material mat = on ? Material.LIME_DYE : Material.GRAY_DYE;
        NamedTextColor color = on ? NamedTextColor.GREEN : NamedTextColor.RED;
        String state = on ? "ВКЛ" : "ВЫКЛ";
        return GuiItems.button(mat,
                Component.text(label, NamedTextColor.WHITE, TextDecoration.BOLD),
                Component.text(state, color),
                Component.text("ЛКМ — переключить", NamedTextColor.GRAY));
    }

    public void togglePerm(int flag) {
        if (ClanPermissions.has(permissions, flag)) {
            permissions &= ~flag;
        } else {
            permissions |= flag;
        }
        fill();
    }

    public int permissions() {
        return permissions;
    }

    public ClanMember target() {
        return target;
    }

    public Player viewer() {
        return viewer;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
