package dev.narek.pveauction.command;

import dev.narek.pveauction.region.AdminRegionService;
import dev.narek.pveauction.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Locale;

public final class ClaimCommand implements CommandExecutor {

    private final AdminRegionService regions;

    public ClaimCommand(AdminRegionService regions) {
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
        if (args.length < 1) {
            Msg.server(player, Msg.err("Использование: /claim <название>"));
            return true;
        }
        try {
            String err = regions.createRegion(player, args[0]);
            if (err != null) {
                Msg.server(player, Msg.err(err));
            } else {
                Msg.server(player, Msg.ok("Регион «" + args[0].toLowerCase(Locale.ROOT) + "» создан."));
            }
        } catch (SQLException e) {
            Msg.server(player, Msg.err("Ошибка БД."));
        }
        return true;
    }
}
