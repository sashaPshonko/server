package dev.narek.pveauction.gui;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.model.AuctionLot;
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

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AuctionMenu implements InventoryHolder {

    public static final int SIZE = 54;
    public static final int LOT_SLOTS = 45;
    public static final int SLOT_STORAGE = 46;
    public static final int SLOT_PREV = 48;
    public static final int SLOT_RELOAD = 49;
    public static final int SLOT_NEXT = 50;

    private final PveAuctionPlugin plugin;
    private final Player viewer;
    private final int page;
    private Inventory inventory;
    private final Map<Integer, Long> slotToLotId = new HashMap<>();

    private AuctionMenu(PveAuctionPlugin plugin, Player viewer, int page) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.page = page;
    }

    public static void open(PveAuctionPlugin plugin, Player player) {
        open(plugin, player, plugin.lastAuctionPage(player.getUniqueId()));
    }

    public static void open(PveAuctionPlugin plugin, Player player, int page) {
        int safePage = Math.max(0, page);
        plugin.setLastAuctionPage(player.getUniqueId(), safePage);
        AuctionMenu menu = new AuctionMenu(plugin, player, safePage);
        menu.inventory = Bukkit.createInventory(menu, SIZE, GuiText.TITLE_AUCTION);
        menu.fill();
        player.openInventory(menu.inventory);
    }

    private void fill() {
        inventory.clear();
        slotToLotId.clear();
        UUID viewerId = viewer.getUniqueId();
        long expiryMs = plugin.auctionExpiryMs();

        int offset = page * LOT_SLOTS;
        List<AuctionLot> lots;
        int totalLots;
        try {
            lots = plugin.lots().listListed(LOT_SLOTS, offset);
            totalLots = plugin.lots().countListedLots();
        } catch (SQLException e) {
            plugin.getLogger().severe("Не загрузить лоты: " + e.getMessage());
            lots = List.of();
            totalLots = 0;
        }

        for (int i = 0; i < lots.size() && i < LOT_SLOTS; i++) {
            AuctionLot lot = lots.get(i);
            boolean own = lot.sellerUuid().equals(viewerId);
            ItemStack display = plugin.lots().bytesToItem(lot.itemBlob());
            if (display == null || display.getType().isAir()) {
                display = new ItemStack(Material.BARRIER);
            }
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                meta.lore(GuiItems.lotLore(lot.price(), lot.sellerName(), own, !own, lot.createdAt(), expiryMs));
                display.setItemMeta(meta);
            }
            inventory.setItem(i, display);
            slotToLotId.put(i, lot.id());
        }

        fillDecorationRow(totalLots);
    }

    private void fillDecorationRow(int totalLots) {
        ItemStack glass = GuiItems.glassFill();
        for (int slot = 45; slot < SIZE; slot++) {
            inventory.setItem(slot, glass);
        }

        int unsold;
        try {
            unsold = plugin.lots().countUnsoldBySeller(viewer.getUniqueId());
        } catch (SQLException e) {
            unsold = 0;
        }
        int max;
        try {
            max = plugin.maxActiveLots(viewer.getUniqueId());
        } catch (SQLException e) {
            max = plugin.maxActiveLotsBase();
        }
        inventory.setItem(SLOT_STORAGE, GuiItems.button(Material.CHEST,
                Component.text("ХРАНИЛИЩЕ", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD),
                Component.text("Твои лоты: " + unsold + "/" + max, NamedTextColor.AQUA)));

        boolean hasPrev = page > 0;
        boolean hasNext = (page + 1) * LOT_SLOTS < totalLots;

        if (hasPrev) {
            inventory.setItem(SLOT_PREV, GuiItems.button(Material.ARROW,
                    Component.text("◀ НАЗАД", NamedTextColor.GREEN, TextDecoration.BOLD),
                    Component.text("Страница " + page, NamedTextColor.GRAY)));
        }

        inventory.setItem(SLOT_RELOAD, GuiItems.button(Material.EMERALD,
                Component.text("ОБНОВИТЬ", NamedTextColor.GREEN, TextDecoration.BOLD),
                Component.text("Стр. " + (page + 1), NamedTextColor.AQUA)));

        if (hasNext) {
            inventory.setItem(SLOT_NEXT, GuiItems.button(Material.ARROW,
                    Component.text("ВПЕРЁД ▶", NamedTextColor.GREEN, TextDecoration.BOLD),
                    Component.text("Страница " + (page + 2), NamedTextColor.GRAY)));
        }
    }

    public void reload() {
        fill();
    }

    public int page() {
        return page;
    }

    public Long lotIdAtSlot(int slot) {
        return slotToLotId.get(slot);
    }

    public Player viewer() {
        return viewer;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
