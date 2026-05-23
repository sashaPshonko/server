package dev.narek.pveauction.command;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.clan.ClanService;
import dev.narek.pveauction.util.Msg;
import dev.narek.pveauction.world.WorldTravelService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class SetHomeCommand implements CommandExecutor {

    private final PveAuctionPlugin plugin;
    private final ClanService clans;
    private final WorldTravelService worlds;

    public SetHomeCommand(PveAuctionPlugin plugin, ClanService clans, WorldTravelService worlds) {
        this.plugin = plugin;
        this.clans = clans;
        this.worlds = worlds;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }
        if (worlds.isSpawnWorld(player.getWorld())) {
            Msg.server(player, Msg.err("На спавне нельзя ставить дом."));
            return true;
        }
        clans.runAsync(player, ok -> {
            if (ok) {
                Msg.server(player, Msg.ok("Дом установлен."));
            }
        }, () -> {
            clans.ensureProfile(player.getUniqueId(), player.getName());
            clans.repo().savePlayerHome(player.getUniqueId(), player.getLocation());
        });
        return true;
    }
}
