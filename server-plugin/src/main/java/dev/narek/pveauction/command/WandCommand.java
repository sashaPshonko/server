package dev.narek.pveauction.command;

import dev.narek.pveauction.region.AdminRegionService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Алиас к //wand */
public final class WandCommand implements CommandExecutor {

    private final AdminRegionService regions;

    public WandCommand(AdminRegionService regions) {
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
        regions.giveWand(player);
        return true;
    }
}
