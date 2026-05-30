package dev.narek.pveauction.command;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.clan.ClanService;
import dev.narek.pveauction.util.MoneyAmounts;
import dev.narek.pveauction.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PayCommand implements CommandExecutor, TabCompleter {

    private final PveAuctionPlugin plugin;
    private final ClanService clans;

    public PayCommand(PveAuctionPlugin plugin, ClanService clans) {
        this.plugin = plugin;
        this.clans = clans;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }
        if (!plugin.economy().isEnabled()) {
            Msg.pay(player, Msg.err("Экономика не подключена."));
            return true;
        }
        if (args.length < 2) {
            Msg.pay(player, Msg.err("Использование: /pay <ник> <сумма>"));
            return true;
        }

        long max = plugin.maxMoneyAmount();
        MoneyAmounts.ParseResult parsed = MoneyAmounts.parse(args[1], max);
        if (!parsed.ok()) {
            Msg.pay(player, Msg.err(parsed.error()));
            return true;
        }
        long amount = parsed.amount();

        String targetName = args[0];
        if (targetName.equalsIgnoreCase(player.getName())) {
            Msg.pay(player, Msg.err("Нельзя перевести себе."));
            return true;
        }

        Player target = clans.findOnline(targetName);
        if (target == null) {
            Msg.pay(player, Msg.err("Игрок должен быть онлайн."));
            return true;
        }

        if (!plugin.economy().has(player, amount)) {
            Msg.pay(player, Msg.err("Не хватает денег."));
            return true;
        }

        if (!plugin.economy().withdraw(player, amount)) {
            Msg.pay(player, Msg.err("Не удалось списать деньги."));
            return true;
        }
        if (!plugin.economy().deposit(target, amount)) {
            plugin.economy().deposit(player, amount);
            Msg.pay(player, Msg.err("Не удалось перевести."));
            return true;
        }

        Msg.pay(player, Msg.ok("Переведено ").append(Msg.money(amount))
                .append(Msg.ok(" → " + target.getName())));
        Msg.pay(target, Msg.info("Получено ").append(Msg.money(amount))
                .append(Msg.info(" от " + player.getName())));
        plugin.scoreboardListener().refreshCoins(player, target);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(p.getName());
                }
            }
            return out;
        }
        return List.of();
    }
}
