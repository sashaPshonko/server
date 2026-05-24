package dev.narek.pveauction.gui.shop;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.shop.ShopCategory;
import dev.narek.pveauction.shop.ShopEntry;
import dev.narek.pveauction.shop.ShopService;
import dev.narek.pveauction.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.sql.SQLException;

public final class ShopGuiListener implements Listener {

    private final PveAuctionPlugin plugin;
    private final ShopService shop;

    public ShopGuiListener(PveAuctionPlugin plugin, ShopService shop) {
        this.plugin = plugin;
        this.shop = shop;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (holder == null) {
            holder = top.getHolder(false);
        }
        if (holder instanceof ShopMainMenu menu) {
            handleMain(event, player, menu);
        } else if (holder instanceof ShopCategoryMenu menu) {
            handleCategories(event, player, menu);
        } else if (holder instanceof ShopSellMenu menu) {
            handleSell(event, player, menu);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        Object holder = event.getView().getTopInventory().getHolder();
        if (holder == null) {
            holder = event.getView().getTopInventory().getHolder(false);
        }
        if (holder instanceof ShopMainMenu
                || holder instanceof ShopCategoryMenu
                || holder instanceof ShopSellMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof ShopMainMenu || holder instanceof ShopCategoryMenu || holder instanceof ShopSellMenu) {
            player.updateInventory();
        }
    }

    private void handleMain(InventoryClickEvent event, Player player, ShopMainMenu menu) {
        event.setCancelled(true);
        if (!player.getUniqueId().equals(menu.viewer().getUniqueId())) {
            return;
        }
        int raw = event.getRawSlot();
        if (raw >= menu.getInventory().getSize()) {
            return;
        }
        if (raw == ShopMainMenu.SLOT_SELL) {
            openCategories(player);
        }
    }

    private void handleCategories(InventoryClickEvent event, Player player, ShopCategoryMenu menu) {
        event.setCancelled(true);
        if (!player.getUniqueId().equals(menu.viewer().getUniqueId())) {
            return;
        }
        int raw = event.getRawSlot();
        if (raw >= menu.getInventory().getSize()) {
            return;
        }

        if (raw == ShopCategoryMenu.SLOT_MODE) {
            shop.cycleSellMode(player);
            try {
                menu.reload();
            } catch (SQLException e) {
                dbError(player, e);
            }
            return;
        }

        if (raw == ShopCategoryMenu.SLOT_BACK) {
            ShopMainMenu.open(player);
            return;
        }

        ShopCategory cat = menu.categoryAt(raw);
        if (cat == null) {
            return;
        }

        if (event.isShiftClick()) {
            try {
                shop.setClanFocus(player, cat);
                Msg.clan(player, Msg.ok("Бонус клана: «" + cat.displayName() + "»"));
                menu.reload();
            } catch (SQLException e) {
                dbError(player, e);
            } catch (IllegalStateException e) {
                Msg.server(player, Msg.err(e.getMessage()));
            }
            return;
        }

        openSell(player, cat);
    }

    private void handleSell(InventoryClickEvent event, Player player, ShopSellMenu menu) {
        event.setCancelled(true);
        if (!player.getUniqueId().equals(menu.viewer().getUniqueId())) {
            return;
        }
        int raw = event.getRawSlot();
        if (raw >= menu.getInventory().getSize()) {
            return;
        }

        if (raw == ShopSellMenu.SLOT_MODE) {
            shop.cycleSellMode(player);
            menu.refresh();
            return;
        }

        if (raw == ShopSellMenu.SLOT_BACK) {
            openCategories(player);
            return;
        }

        ShopEntry entry = menu.entryAt(raw);
        if (entry == null) {
            return;
        }

        ShopCategory cat = menu.category();
        try {
            ShopService.SellResult result = shop.sell(player, cat, entry.material(), entry.basePrice());
            result.send(player);
            if (result.success()) {
                openSell(player, cat);
            }
        } catch (SQLException e) {
            dbError(player, e);
        }
    }

    private void openCategories(Player player) {
        try {
            ShopCategoryMenu.open(plugin, shop, player);
        } catch (SQLException e) {
            dbError(player, e);
        }
    }

    private void openSell(Player player, ShopCategory category) {
        try {
            double mult = 1.0;
            var member = plugin.clans().repo().findMember(player.getUniqueId());
            if (member.isPresent()) {
                mult = shop.clanMultiplier(member.get().clanId(), category);
            }
            ShopSellMenu.open(plugin, shop, player, category, mult);
        } catch (SQLException e) {
            dbError(player, e);
        }
    }

    private void dbError(Player player, SQLException e) {
        plugin.getLogger().severe(e.getMessage());
        Msg.server(player, Msg.err("Ошибка базы данных."));
    }
}
