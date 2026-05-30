package dev.narek.pveauction.command;

import dev.narek.pveauction.travel.TeleportRequestService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class TpAcceptCommand implements CommandExecutor {

    private final TeleportRequestService tp;

    public TpAcceptCommand(TeleportRequestService tp) {
        this.tp = tp;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (sender instanceof Player player) {
            tp.accept(player);
        }
        return true;
    }
}
