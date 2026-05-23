package dev.narek.pveauction.listener;

import dev.narek.pveauction.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;
import java.util.Set;

public final class CommandWhitelistListener implements Listener {

    private static final Set<String> ALLOWED = Set.of("ah", "admin");

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().trim();
        if (!message.startsWith("/")) {
            return;
        }
        String label = message.substring(1).split("\\s+")[0].toLowerCase(Locale.ROOT);
        int colon = label.indexOf(':');
        if (colon >= 0) {
            label = label.substring(colon + 1);
        }
        if (ALLOWED.contains(label)) {
            return;
        }
        event.setCancelled(true);
        Msg.send(event.getPlayer(), Msg.err("Доступны только /ah и /admin."));
    }
}
