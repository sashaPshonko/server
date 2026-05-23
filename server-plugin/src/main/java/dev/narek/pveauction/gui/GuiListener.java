package dev.narek.pveauction.gui;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.model.AuctionLot;
import dev.narek.pveauction.util.Msg;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Optional;

public final class GuiListener implements Listener {

    private final PveAuctionPlugin plugin;

    public GuiListener(PveAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        Object holder = top.getHolder();

        if (holder instanceof AuctionMenu menu) {
            handleAuctionClick(event, menu);
        } else if (holder instanceof StorageMenu menu) {
            handleStorageClick(event, menu);
        } else if (holder instanceof AdminMenu menu) {
            handleAdminClick(event, menu);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Object holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof AuctionMenu || holder instanceof StorageMenu || holder instanceof AdminMenu) {
            event.setCancelled(true);
        }
    }

    private void handleAuctionClick(InventoryClickEvent event, AuctionMenu menu) {
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

        if (raw == AuctionMenu.SLOT_PREV) {
            AuctionMenu.open(plugin, player, menu.page() - 1);
            return;
        }
        if (raw == AuctionMenu.SLOT_NEXT) {
            AuctionMenu.open(plugin, player, menu.page() + 1);
            return;
        }
        if (raw == AuctionMenu.SLOT_RELOAD) {
            menu.reload();
            Msg.send(player, Msg.ok("Список обновлён."));
            return;
        }
        if (raw == AuctionMenu.SLOT_STORAGE) {
            StorageMenu.open(plugin, player);
            return;
        }

        Long lotId = menu.lotIdAtSlot(raw);
        if (lotId == null) {
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Optional<AuctionLot> lotOpt = plugin.lots().findListed(lotId);
                if (lotOpt.isEmpty()) {
                    runSync(player, () -> {
                        Msg.send(player, Msg.err("Лот уже недоступен."));
                        refreshOpenMenu(player);
                    });
                    return;
                }
                AuctionLot lot = lotOpt.get();
                if (lot.sellerUuid().equals(player.getUniqueId())) {
                    cancelOwnLot(player, lotId);
                } else {
                    runSync(player, () -> tryBuy(player, lotId));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe(e.getMessage());
                runSync(player, () -> Msg.send(player, Msg.err("Ошибка базы данных.")));
            }
        });
    }

    private void handleStorageClick(InventoryClickEvent event, StorageMenu menu) {
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

        if (raw == StorageMenu.SLOT_BACK) {
            AuctionMenu.open(plugin, player);
            return;
        }
        if (raw == StorageMenu.SLOT_RELIST) {
            tryRelist(player);
            return;
        }

        Long lotId = menu.lotIdAtSlot(raw);
        if (lotId != null) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> cancelOwnLot(player, lotId));
        }
    }

    private void handleAdminClick(InventoryClickEvent event, AdminMenu menu) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!player.hasPermission("pveauction.admin")) {
            return;
        }
        if (!player.getUniqueId().equals(menu.viewer().getUniqueId())) {
            return;
        }
        int raw = event.getRawSlot();
        if (raw >= menu.getInventory().getSize()) {
            return;
        }

        if (raw == AdminMenu.SLOT_CREATIVE) {
            if (player.getGameMode() == GameMode.CREATIVE) {
                player.setGameMode(GameMode.SURVIVAL);
                Msg.send(player, Msg.warn("Креатив выключен."));
            } else {
                player.setGameMode(GameMode.CREATIVE);
                Msg.send(player, Msg.ok("Креатив включён."));
            }
            menu.reload();
            return;
        }
        if (raw == AdminMenu.SLOT_GIVE_10K) {
            giveMoney(player, player, 10_000);
            return;
        }
        if (raw == AdminMenu.SLOT_GIVE_100K) {
            giveMoney(player, player, 100_000);
        }
    }

    private void tryRelist(Player player) {
        long left = plugin.relistCooldownLeftMs(player.getUniqueId());
        if (left > 0) {
            runSync(player, () -> {
                StorageMenu.open(plugin, player);
                Msg.send(player, Msg.err("Перевыставление через " + (left / 1000) + " сек."));
            });
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                int n = plugin.lots().relistSellerLots(player.getUniqueId());
                plugin.markRelistUsed(player.getUniqueId());
                runSync(player, () -> {
                    Msg.send(player, Msg.ok("Перевыставлено лотов: " + n));
                    StorageMenu.open(plugin, player);
                });
            } catch (SQLException e) {
                plugin.getLogger().severe(e.getMessage());
                runSync(player, () -> Msg.send(player, Msg.err("Ошибка базы данных.")));
            }
        });
    }

    private void cancelOwnLot(Player player, long lotId) {
        try {
            Optional<AuctionLot> cancelled = plugin.lots().cancelActiveLot(lotId, player.getUniqueId());
            runSync(player, () -> {
                if (cancelled.isEmpty()) {
                    Msg.send(player, Msg.err("Не удалось снять лот."));
                } else {
                    ItemStack item = plugin.lots().bytesToItem(cancelled.get().itemBlob());
                    giveItem(player, item);
                    Msg.send(player, Msg.warn("Лот снят с аукциона."));
                }
                refreshOpenMenu(player);
            });
        } catch (SQLException e) {
            plugin.getLogger().severe(e.getMessage());
            runSync(player, () -> Msg.send(player, Msg.err("Ошибка базы данных.")));
        }
    }

    private void tryBuy(Player buyer, long lotId) {
        Optional<AuctionLot> opt;
        try {
            opt = plugin.lots().findListed(lotId);
        } catch (SQLException e) {
            Msg.send(buyer, Msg.err("Ошибка базы данных."));
            return;
        }
        if (opt.isEmpty()) {
            Msg.send(buyer, Msg.err("Лот уже купили."));
            refreshOpenMenu(buyer);
            return;
        }

        AuctionLot lot = opt.get();
        if (lot.sellerUuid().equals(buyer.getUniqueId())) {
            Msg.send(buyer, Msg.info("Это твой лот — нажми, чтобы снять."));
            return;
        }

        double price = lot.price();
        if (plugin.economy().isEnabled()) {
            if (!plugin.economy().has(buyer, price)) {
                Msg.send(buyer, Msg.err("Не хватает денег."));
                return;
            }
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (!plugin.lots().tryMarkSold(lotId, buyer.getUniqueId())) {
                    runSync(buyer, () -> {
                        Msg.send(buyer, Msg.err("Лот уже купили."));
                        refreshOpenMenu(buyer);
                    });
                    return;
                }

                if (plugin.economy().isEnabled()) {
                    plugin.economy().withdraw(buyer, price);
                    Player seller = plugin.getServer().getPlayer(lot.sellerUuid());
                    if (seller != null && seller.isOnline()) {
                        plugin.economy().deposit(seller, price);
                    } else {
                        plugin.economy().deposit(
                                plugin.getServer().getOfflinePlayer(lot.sellerUuid()), price);
                    }
                }

                ItemStack item = plugin.lots().bytesToItem(lot.itemBlob());
                runSync(buyer, () -> {
                    giveItem(buyer, item);
                    Msg.send(buyer, Msg.ok("Куплено за ").append(Msg.money((long) price)));
                    refreshOpenMenu(buyer);
                });
            } catch (SQLException e) {
                plugin.getLogger().severe(e.getMessage());
                runSync(buyer, () -> Msg.send(buyer, Msg.err("Ошибка покупки.")));
            }
        });
    }

    private void giveMoney(Player admin, Player target, double amount) {
        if (!plugin.economy().isEnabled()) {
            Msg.send(admin, Msg.err("Экономика не подключена (Vault + Essentials)."));
            return;
        }
        if (plugin.economy().deposit(target, amount)) {
            Msg.send(admin, Msg.ok("Выдано ").append(Msg.money(amount))
                    .append(Msg.ok(" → " + target.getName())));
        } else {
            Msg.send(admin, Msg.err("Не удалось выдать деньги."));
        }
    }

    private void giveItem(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            Msg.send(player, Msg.warn("Инвентарь полон — часть предметов на земле."));
        }
    }

    private void refreshOpenMenu(Player player) {
        Inventory top = player.getOpenInventory().getTopInventory();
        Object holder = top.getHolder();
        if (holder instanceof AuctionMenu m) {
            m.reload();
        } else if (holder instanceof StorageMenu m) {
            m.reload();
        }
    }

    private void runSync(Player player, Runnable task) {
        plugin.getServer().getScheduler().runTask(plugin, task);
    }
}
