package dev.narek.pveauction.command;

import dev.narek.pveauction.auth.AuthService;
import dev.narek.pveauction.util.TravelMsg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class LoginCommand implements CommandExecutor {

    private final AuthService auth;

    public LoginCommand(AuthService auth) {
        this.auth = auth;
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
        if (args.length < 1) {
            TravelMsg.send(player, TravelMsg.err("Использование: /login <пароль>  (или /l)"));
            return true;
        }
        auth.completeLogin(player, args[0].toCharArray(), false);
        return true;
    }
}
