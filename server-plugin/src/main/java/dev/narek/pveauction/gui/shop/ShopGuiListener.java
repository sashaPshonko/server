package dev.narek.pveauction.gui.shop;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.shop.ShopCategory;
import dev.narek.pveauction.shop.ShopEntry;
import dev.narek.pveauction.shop.ShopService;
import dev.narek.pveauction.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.sql.SQLException;

public final class ShopGuiListener implements Listener {

    private final PveAuctionPlugin plugin;
    private final ShopService shop;

    public ShopGuiListener(PveAuctionPlugin plugin, ShopService shop) {
        this.plugin = plugin;
        this.shop = shop;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        Object holder = top.getHolder();
        if (holder instanceof ShopMainMenu menu) {
            handleMain(event, menu);
        } else if (holder instanceof ShopCategoryMenu menu) {
            handleCategories(event, menu);
        } else if (holder instanceof ShopSellMenu menu) {
            handleSell(event, menu);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Object holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof ShopMainMenu || holder instanceof ShopCategoryMenu || holder instanceof ShopSellMenu) {
            event.setCancelled(true);
        }
    }

    private void handleMain(InventoryClickEvent event, ShopMainMenu menu) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!player.getUniqueId().equals(menu.viewer().getUniqueId())) {
            return;
        }
        if (event.getRawSlot() == ShopMainMenu.SLOT_SELL) {
            runDb(player, () -> ShopCategoryMenu.open(plugin, shop, player));
        }
    }

    private void handleCategories(InventoryClickEvent event, ShopCategoryMenu menu) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!player.getUniqueId().equals(menu.viewer().getUniqueId())) {
            return;
        }
        int raw = event.getRawSlot();
        if (raw >= menu.getInventory().getSize()) {
            return;
        }

        if (raw == ShopCategoryMenu.SLOT_MODE) {
            shop.cycleSellMode(player);
            runDb(player, menu::reload);
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
            runDb(player, () -> {
                shop.setClanFocus(player, cat);
                runSync(() -> {
                    Msg.clan(player, Msg.ok("Бонус клана на категорию «" + cat.displayName() + "»."));
                    try {
                        menu.reload();
                    } catch (SQLException e) {
                        dbError(player, e);
                    }
                });
            });
            return;
        }

        runDb(player, () -> {
            double mult = 1.0;
            var member = plugin.clans().repo().findMember(player.getUniqueId());
            if (member.isPresent()) {
                mult = shop.clanMultiplier(member.get().clanId(), cat);
            }
            double finalMult = mult;
            runSync(() -> ShopSellMenu.open(plugin, shop, player, cat, finalMult));
        });
    }

    private void handleSell(InventoryClickEvent event, ShopSellMenu menu) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
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
            runDb(player, () -> ShopCategoryMenu.open(plugin, shop, player));
            return;
        }

        ShopEntry entry = menu.entryAt(raw);
        if (entry == null) {
            return;
        }

        ShopCategory cat = menu.category();
        runDb(player, () -> {
            ShopService.SellResult result = shop.sell(player, cat, entry.material(), entry.basePrice());
            runSync(() -> {
                result.send(player);
                if (!result.success()) {
                    return;
                }
                try {
                    double mult = 1.0;
                    var member = plugin.clans().repo().findMember(player.getUniqueId());
                    if (member.isPresent()) {
                        mult = shop.clanMultiplier(member.get().clanId(), cat);
                    }
                    ShopSellMenu.open(plugin, shop, player, cat, mult);
                } catch (SQLException e) {
                    dbError(player, e);
                }
            });
        });
    }

    @FunctionalInterface
    private interface DbTask {
        void run() throws SQLException;
    }

    private void runDb(Player player, DbTask task) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                task.run();
            } catch (SQLException e) {
                dbError(player, e);
            } catch (IllegalStateException e) {
                runSync(() -> Msg.server(player, Msg.err(e.getMessage())));
            }
        });
    }

    private void runSync(Runnable task) {
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    private void dbError(Player player, SQLException e) {
        plugin.getLogger().severe(e.getMessage());
        runSync(() -> Msg.server(player, Msg.err("Ошибка базы данных.")));
    }
}
