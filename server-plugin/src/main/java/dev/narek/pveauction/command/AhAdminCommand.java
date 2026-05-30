package dev.narek.pveauction.command;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.gui.AdminMenu;
import dev.narek.pveauction.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class AhAdminCommand implements CommandExecutor, TabCompleter {

    private final PveAuctionPlugin plugin;

    public AhAdminCommand(PveAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pveauction.admin")) {
            Msg.server(sender, Msg.err("Нет прав администратора."));
            return true;
        }

        if (args.length >= 3 && args[0].equalsIgnoreCase("give")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                Msg.server(sender, Msg.err("Игрок не в сети."));
                return true;
            }
            double amount;
            try {
                amount = Double.parseDouble(args[2].replace(" ", "").replace("_", ""));
            } catch (NumberFormatException e) {
                Msg.server(sender, Msg.err("Сумма: /admin give <ник> <число>"));
                return true;
            }
            if (amount <= 0) {
                Msg.server(sender, Msg.err("Сумма должна быть больше нуля."));
                return true;
            }
            if (!plugin.economy().isEnabled()) {
                Msg.server(sender, Msg.err("Экономика не подключена (Vault + Essentials)."));
                return true;
            }
            if (plugin.economy().deposit(target, amount)) {
                Msg.server(sender, Msg.ok("Выдано ")
                        .append(Msg.money(amount))
                        .append(Msg.ok(" → " + target.getName())));
                if (target.isOnline() && plugin.scoreboardListener() != null) {
                    plugin.scoreboardListener().refreshCoins(target);
                }
            } else {
                Msg.server(sender, Msg.err("Не удалось выдать деньги."));
            }
            return true;
        }

        if (sender instanceof Player player) {
            AdminMenu.open(player);
            return true;
        }

        sender.sendMessage("Использование: /admin | /admin give <ник> <сумма>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("pveauction.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            if ("give".startsWith(args[0].toLowerCase())) {
                out.add("give");
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            String prefix = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
