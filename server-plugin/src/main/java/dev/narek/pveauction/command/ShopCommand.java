package dev.narek.pveauction.command;

import dev.narek.pveauction.gui.shop.ShopMainMenu;
import dev.narek.pveauction.gui.shop.ShopSellMenu;
import dev.narek.pveauction.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ShopCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }
        Msg.shop(player, Msg.ok("PveAuction скупка, сборка " + ShopSellMenu.LAYOUT_BUILD));
        ShopMainMenu.open(player);
        return true;
    }
}
