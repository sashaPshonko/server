package dev.narek.pveauction.command;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.gui.shop.ShopMainMenu;
import dev.narek.pveauction.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ShopCommand implements CommandExecutor {

    private final PveAuctionPlugin plugin;

    public ShopCommand(PveAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }
        Msg.shop(player, Msg.ok("PveAuction скупка, сборка " + plugin.getPluginMeta().getVersion()));
        ShopMainMenu.open(player);
        return true;
    }
}
