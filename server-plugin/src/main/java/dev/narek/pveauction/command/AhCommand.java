package dev.narek.pveauction.command;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.gui.AuctionMenu;
import dev.narek.pveauction.util.GuiItems;
import dev.narek.pveauction.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class AhCommand implements CommandExecutor, TabCompleter {

    private final PveAuctionPlugin plugin;

    public AhCommand(PveAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }
        if (!player.hasPermission("pveauction.use")) {
            Msg.send(player, Msg.err("Нет доступа к аукциону."));
            return true;
        }

        if (args.length == 0) {
            AuctionMenu.open(plugin, player);
            return true;
        }

        if (args[0].equalsIgnoreCase("sell") && args.length >= 2) {
            long maxPrice = plugin.maxAuctionPrice();
            long price;
            try {
                price = Long.parseLong(args[1].replace(" ", "").replace("_", ""));
            } catch (NumberFormatException e) {
                Msg.send(player, Msg.err("Максимальная цена — " + GuiItems.formatPrice(maxPrice) + " $"));
                return true;
            }
            if (price < 1) {
                Msg.send(player, Msg.err("Цена должна быть больше нуля."));
                return true;
            }
            if (price > maxPrice) {
                Msg.send(player, Msg.err("Максимальная цена — " + GuiItems.formatPrice(maxPrice) + " $"));
                return true;
            }

            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType().isAir()) {
                Msg.send(player, Msg.err("Возьми предмет в руку."));
                return true;
            }

            ItemStack toStore = hand.clone();
            hand.setAmount(0);

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    int max = plugin.maxActiveLots(player.getUniqueId());
                    int active = plugin.lots().countUnsoldBySeller(player.getUniqueId());
                    if (active >= max) {
                        runSync(player, () -> {
                            player.getInventory().addItem(toStore);
                            Msg.send(player, Msg.err("Лимит " + max + " лотов. Освободи место в хранилище."));
                        });
                        return;
                    }

                    plugin.lots().insertLot(
                            player.getUniqueId(),
                            player.getName(),
                            toStore,
                            price
                    );
                    int slotNum = active + 1;
                    runSync(player, () -> {
                        Msg.send(player, Msg.ok("Выставлено (" + slotNum + "/" + max + ") за ")
                                .append(Msg.money(price)));
                        refreshMenu(player);
                    });
                } catch (SQLException e) {
                    plugin.getLogger().severe(e.getMessage());
                    runSync(player, () -> Msg.send(player, Msg.err("Ошибка базы данных.")));
                }
            });
            return true;
        }

        Msg.send(player, Msg.info("Использование: /ah | /ah sell <цена>"));
        return true;
    }

    private void refreshMenu(Player player) {
        var holder = player.getOpenInventory().getTopInventory().getHolder();
        if (holder instanceof AuctionMenu menu) {
            menu.reload();
        }
    }

    private void runSync(Player player, Runnable task) {
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            if ("sell".startsWith(args[0].toLowerCase())) {
                out.add("sell");
            }
            return out;
        }
        return List.of();
    }
}
