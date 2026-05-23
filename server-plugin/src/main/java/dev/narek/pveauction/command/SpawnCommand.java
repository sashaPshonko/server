package dev.narek.pveauction.command;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.util.TravelMsg;
import dev.narek.pveauction.world.WorldTeleportService;
import dev.narek.pveauction.world.WorldTravelService;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class SpawnCommand implements CommandExecutor {

    private final PveAuctionPlugin plugin;
    private final WorldTravelService worlds;

    public SpawnCommand(PveAuctionPlugin plugin) {
        this.plugin = plugin;
        this.worlds = plugin.worlds();
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
        if (!player.hasPermission("pveauction.travel.spawn")) {
            TravelMsg.send(player, TravelMsg.err("Нет доступа."));
            return true;
        }

        Location target = worlds.spawnLocation();
        if (target.getWorld() == null) {
            TravelMsg.send(player, TravelMsg.err("Мир спавна ещё не загружен."));
            return true;
        }

        WorldTeleportService.teleport(plugin, player, target, ok -> {
            if (!ok) {
                TravelMsg.send(player, TravelMsg.err("Не удалось телепортироваться."));
                return;
            }
            worlds.applySpawnRules(player);
            TravelMsg.send(player, TravelMsg.ok("Ты на спавне."));
        });
        return true;
    }
}
