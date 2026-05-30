package dev.narek.pveauction.command;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.item.CustomItems;
import dev.narek.pveauction.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class GiveSilverCommand implements CommandExecutor, TabCompleter {

    private final PveAuctionPlugin plugin;

    public GiveSilverCommand(PveAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pveauction.admin")) {
            Msg.server(sender, Msg.err("Нет прав."));
            return true;
        }
        if (args.length < 2 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage("Использование: /givesilver give <ник> <кол-во>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            Msg.server(sender, Msg.err("Игрок не в сети."));
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[2].replace(" ", "").replace("_", ""));
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            Msg.server(sender, Msg.err("Кол-во: /givesilver give <ник> <число>"));
            return true;
        }
        if (amount <= 0) {
            Msg.server(sender, Msg.err("Кол-во должно быть больше нуля."));
            return true;
        }
        int given = CustomItems.addSilver(plugin, target, amount);
        if (given < amount) {
            Msg.server(sender, Msg.warn("Выдано " + given + " из " + amount + " (инвентарь полон)."));
        } else {
            Msg.server(sender, Msg.ok("Серебро ×" + amount + " → " + target.getName()));
        }
        Msg.server(target, Msg.ok("Получено серебра: " + given));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("pveauction.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return List.of("give");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    names.add(p.getName());
                }
            }
            return names;
        }
        return List.of();
    }
}
