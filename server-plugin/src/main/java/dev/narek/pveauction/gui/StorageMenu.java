package dev.narek.pveauction.gui;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.model.AuctionLot;
import dev.narek.pveauction.util.GuiItems;
import dev.narek.pveauction.util.GuiText;
import dev.narek.pveauction.util.LotExpiry;
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

public final class StorageMenu implements InventoryHolder {

    public static final int SIZE = 54;
    public static final int LOT_SLOTS = 45;
    public static final int SLOT_BACK = 46;
    public static final int SLOT_RELIST = 49;

    private final PveAuctionPlugin plugin;
    private final Player viewer;
    private Inventory inventory;
    private final Map<Integer, Long> slotToLotId = new HashMap<>();

    private StorageMenu(PveAuctionPlugin plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    public static void open(PveAuctionPlugin plugin, Player player) {
        StorageMenu menu = new StorageMenu(plugin, player);
        menu.inventory = Bukkit.createInventory(menu, SIZE, GuiText.TITLE_STORAGE);
        menu.fill();
        player.openInventory(menu.inventory);
    }

    private void fill() {
        inventory.clear();
        slotToLotId.clear();
        long expiryMs = plugin.auctionExpiryMs();

        List<AuctionLot> lots;
        try {
            int maxLots = plugin.maxActiveLots(viewer.getUniqueId());
            lots = plugin.lots().listUnsoldBySeller(viewer.getUniqueId(), maxLots);
        } catch (SQLException e) {
            plugin.getLogger().severe("Не загрузить хранилище: " + e.getMessage());
            lots = List.of();
        }

        for (int i = 0; i < lots.size() && i < LOT_SLOTS; i++) {
            AuctionLot lot = lots.get(i);
            ItemStack display = plugin.lots().bytesToItem(lot.itemBlob());
            if (display == null || display.getType().isAir()) {
                display = new ItemStack(Material.BARRIER);
            }
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                meta.lore(GuiItems.lotLore(lot.price(), lot.sellerName(), true, false, lot.createdAt(), expiryMs));
                display.setItemMeta(meta);
            }
            inventory.setItem(i, display);
            slotToLotId.put(i, lot.id());
        }

        long cooldownSec = plugin.relistCooldownMs() / 1000;
        long left = plugin.relistCooldownLeftMs(viewer.getUniqueId());

        ItemStack glass = GuiItems.glassFill();
        for (int slot = 45; slot < SIZE; slot++) {
            if (slot == SLOT_BACK || slot == SLOT_RELIST) {
                continue;
            }
            inventory.setItem(slot, glass);
        }

        inventory.setItem(SLOT_BACK, GuiItems.button(Material.ARROW,
                Component.text("НА АУКЦИОН", NamedTextColor.GREEN, TextDecoration.BOLD)));

        boolean hasExpired = lots.stream().anyMatch(lot -> LotExpiry.isExpired(lot, expiryMs));

        Component relistName = Component.text("ПЕРЕВЫСТАВИТЬ", NamedTextColor.AQUA, TextDecoration.BOLD);
        Component relistStatus;
        if (left > 0) {
            relistStatus = Component.text("Через " + (left / 1000) + " сек.", NamedTextColor.RED);
        } else if (hasExpired) {
            relistStatus = Component.text("Сбросить таймер истёкших!", NamedTextColor.GOLD, TextDecoration.BOLD);
        } else {
            relistStatus = Component.text("Доступно перевыставление!", NamedTextColor.GREEN, TextDecoration.BOLD);
        }

        inventory.setItem(SLOT_RELIST, GuiItems.button(Material.CLOCK, relistName, relistStatus,
                Component.text("Раз в " + cooldownSec + " сек.", NamedTextColor.GRAY),
                Component.text("Срок лота: " + (plugin.getConfig().getLong("auction-expiry-hours", 12)) + " ч.",
                        NamedTextColor.GRAY)));
    }

    public void reload() {
        fill();
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
