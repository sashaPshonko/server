package dev.narek.pveauction.command;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.clan.ClanService;
import dev.narek.pveauction.model.SavedLocation;
import dev.narek.pveauction.util.Msg;
import dev.narek.pveauction.world.WorldTeleportService;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class HomeCommand implements CommandExecutor {

    private final PveAuctionPlugin plugin;
    private final ClanService clans;

    public HomeCommand(PveAuctionPlugin plugin, ClanService clans) {
        this.plugin = plugin;
        this.clans = clans;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }
        clans.runAsync(player, ok -> {}, () -> {
            clans.ensureProfile(player.getUniqueId(), player.getName());
            Optional<SavedLocation> home = clans.repo().findPlayerHome(player.getUniqueId());
            clans.runSync(player, () -> {
                if (home.isEmpty()) {
                    Msg.send(player, Msg.err("Сначала /sethome."));
                    return;
                }
                Location target = home.get().toLocation();
                if (target == null) {
                    Msg.send(player, Msg.err("Мир дома не загружен."));
                    return;
                }
                WorldTeleportService.teleport(plugin, player, target, success -> {
                    if (success) {
                        Msg.send(player, Msg.ok("Телепорт домой."));
                    } else {
                        Msg.send(player, Msg.err("Не удалось телепортироваться."));
                    }
                });
            });
        });
        return true;
    }
}
