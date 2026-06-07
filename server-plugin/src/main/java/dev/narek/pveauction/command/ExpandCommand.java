package dev.narek.pveauction.command;

import dev.narek.pveauction.region.AdminRegionService;
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

public final class ExpandCommand implements CommandExecutor, TabCompleter {

    private final AdminRegionService regions;

    public ExpandCommand(AdminRegionService regions) {
        this.regions = regions;
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
        if (!regions.canManage(player)) {
            Msg.server(player, Msg.err("Нет доступа."));
            return true;
        }
        if (args.length < 2) {
            Msg.server(player, Msg.err("Использование: /expand <число> up|down"));
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            Msg.server(player, Msg.err("Укажи число блоков, например 20."));
            return true;
        }
        String err = regions.expand(player, amount, args[1]);
        if (err != null) {
            Msg.server(player, Msg.err(err));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player) || !regions.canManage(player)) {
            return List.of();
        }
        if (args.length == 1) {
            return List.of("5", "10", "20", "50");
        }
        if (args.length == 2) {
            return filter(args[1], "up", "down");
        }
        return List.of();
    }

    private static List<String> filter(String prefix, String... options) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.startsWith(p)) {
                out.add(o);
            }
        }
        return out;
    }
}
