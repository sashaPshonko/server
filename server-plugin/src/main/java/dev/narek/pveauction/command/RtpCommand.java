package dev.narek.pveauction.command;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.util.TravelMsg;
import dev.narek.pveauction.world.WorldTravelService;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class RtpCommand implements CommandExecutor {

    private final PveAuctionPlugin plugin;
    private final WorldTravelService worlds;

    public RtpCommand(PveAuctionPlugin plugin) {
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
        if (!player.hasPermission("pveauction.travel.rtp")) {
            TravelMsg.send(player, TravelMsg.err("Нет доступа."));
            return true;
        }
        if (plugin.getConfig().getBoolean("rtp-only-from-spawn-world", true)
                && !worlds.isSpawnWorld(player.getWorld())) {
            TravelMsg.send(player, TravelMsg.err("Команда /rtp работает только на спавне."));
            return true;
        }

        Location target = worlds.rtpLocation();
        if (target.getWorld() == null) {
            TravelMsg.send(player, TravelMsg.err("Мир RTP ещё не загружен."));
            return true;
        }

        player.teleportAsync(target).thenAccept(ok -> {
            if (!ok) {
                TravelMsg.send(player, TravelMsg.err("Не удалось телепортироваться."));
                return;
            }
            worlds.applyRtpRules(player);
            TravelMsg.send(player, TravelMsg.ok("Ты в мире RTP."));
        });
        return true;
    }
}
