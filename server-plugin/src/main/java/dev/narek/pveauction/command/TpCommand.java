package dev.narek.pveauction.command;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.travel.TeleportRequestService;
import dev.narek.pveauction.util.Msg;
import org.bukkit.Bukkit;
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

public final class TpCommand implements CommandExecutor, TabCompleter {

    private final PveAuctionPlugin plugin;
    private final TeleportRequestService tp;

    public TpCommand(PveAuctionPlugin plugin, TeleportRequestService tp) {
        this.plugin = plugin;
        this.tp = tp;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }
        if (!player.hasPermission("pveauction.tpa")) {
            Msg.tp(player, Msg.err("Нет доступа."));
            return true;
        }
        if (args.length < 1) {
            Msg.tp(player, Msg.err("Использование: /tpa <ник>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            Msg.tp(player, Msg.err("Игрок не в сети."));
            return true;
        }

        tp.send(player, target);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length != 1 || !(sender instanceof Player self)) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(self)) {
                continue;
            }
            String name = online.getName();
            if (name != null && name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                out.add(name);
            }
        }
        return out;
    }
}
