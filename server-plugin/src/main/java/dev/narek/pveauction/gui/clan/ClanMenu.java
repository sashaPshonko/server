package dev.narek.pveauction.gui.clan;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.clan.ClanService;
import dev.narek.pveauction.model.ClanData;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ClanMenu implements InventoryHolder {

    public static final int SIZE = 54;
    public static final int SLOT_INVITE = 48;
    public static final int SLOT_INVEST = 49;
    public static final int SLOT_WITHDRAW = 50;
    public static final int SLOT_SETHOME = 51;

    private final PveAuctionPlugin plugin;
    private final Player viewer;
    private final ClanData clan;
    private final ClanMember viewerMember;
    private final List<ClanMember> members;
    private Inventory inventory;
    private final Map<Integer, UUID> slotToMember = new HashMap<>();

    private ClanMenu(PveAuctionPlugin plugin, Player viewer, ClanData clan, ClanMember viewerMember, List<ClanMember> members) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.clan = clan;
        this.viewerMember = viewerMember;
        this.members = members;
    }

    public static void open(PveAuctionPlugin plugin, Player player, ClanData clan, ClanMember viewerMember, List<ClanMember> members) {
        ClanMenu menu = new ClanMenu(plugin, player, clan, viewerMember, members);
        menu.inventory = Bukkit.createInventory(menu, SIZE, GuiText.title("Клан: " + clan.name(), NamedTextColor.DARK_AQUA));
        menu.fill();
        player.openInventory(menu.inventory);
    }

    private void fill() {
        inventory.clear();
        slotToMember.clear();
        ItemStack glass = GuiItems.glassFill();
        for (int i = 45; i < SIZE; i++) {
            inventory.setItem(i, glass);
        }

        int slot = 0;
        for (ClanMember member : members) {
            if (slot >= 45) {
                break;
            }
            inventory.setItem(slot, memberHead(member));
            slotToMember.put(slot, member.playerUuid());
            slot++;
        }

        if (viewerMember.can(ClanPermissions.INVITE)) {
            inventory.setItem(SLOT_INVITE, GuiItems.button(Material.WRITABLE_BOOK,
                    Component.text("ПРИГЛАСИТЬ", NamedTextColor.GREEN, TextDecoration.BOLD),
                    Component.text("/clan invite <ник>", NamedTextColor.GRAY)));
        }
        inventory.setItem(SLOT_INVEST, GuiItems.button(Material.GOLD_INGOT,
                Component.text("ВЛОЖИТЬ", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text("/clan invest <сумма>", NamedTextColor.GRAY)));
        if (viewerMember.can(ClanPermissions.WITHDRAW)) {
            inventory.setItem(SLOT_WITHDRAW, GuiItems.button(Material.EMERALD,
                    Component.text("СНЯТЬ", NamedTextColor.AQUA, TextDecoration.BOLD),
                    Component.text("/clan withdraw <сумма>", NamedTextColor.GRAY)));
        }
        if (viewerMember.isOwner()) {
            inventory.setItem(SLOT_SETHOME, GuiItems.button(Material.RED_BED,
                    Component.text("КЛАН-ДОМ", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD),
                    Component.text("/clan sethome | /clan home", NamedTextColor.GRAY)));
        }

        inventory.setItem(53, GuiItems.button(Material.SUNFLOWER,
                Component.text("Казна: " + GuiItems.formatPrice(clan.balance()) + " $", NamedTextColor.YELLOW, TextDecoration.BOLD),
                Component.text("/clan money", NamedTextColor.GRAY)));
    }

    private ItemStack memberHead(ClanMember member) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(member.playerUuid()));
            String role = member.isOwner() ? "Владелец" : "Участник";
            meta.displayName(Component.text(member.playerName(), NamedTextColor.WHITE, TextDecoration.BOLD));
            meta.lore(List.of(
                    Component.text(role, member.isOwner() ? NamedTextColor.GOLD : NamedTextColor.GRAY),
                    Component.text("Права: " + ClanService.permLabel(member.permissions()), NamedTextColor.AQUA),
                    Component.empty(),
                    Component.text(viewerMember.isOwner() && !member.isOwner()
                            ? "ЛКМ — настроить права" : " ", NamedTextColor.YELLOW)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    public UUID memberAt(int slot) {
        return slotToMember.get(slot);
    }

    public ClanMember viewerMember() {
        return viewerMember;
    }

    public Player viewer() {
        return viewer;
    }

    public void reload(ClanData updated, List<ClanMember> updatedMembers) {
        open(plugin, viewer, updated, viewerMember, updatedMembers);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
