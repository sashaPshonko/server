package dev.narek.pveauction.listener;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.chat.ChatService;
import dev.narek.pveauction.model.PlayerProfile;
import dev.narek.pveauction.util.Msg;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.sql.SQLException;

public final class ChatListener implements Listener {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final PveAuctionPlugin plugin;
    private final ChatService chat;

    public ChatListener(PveAuctionPlugin plugin, ChatService chat) {
        this.plugin = plugin;
        this.chat = chat;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat.enabled", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (plugin.auth() != null && !plugin.auth().isLoggedIn(player)) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        String raw = PLAIN.serialize(event.message()).trim();
        if (raw.isEmpty()) {
            return;
        }

        boolean global = raw.startsWith("!");
        String text = global ? raw.substring(1).trim() : raw;
        if (text.isEmpty()) {
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> Msg.server(player, Msg.err("Напиши текст после «!» для мирового чата.")));
            return;
        }

        try {
            PlayerProfile profile = plugin.players().getOrCreate(player.getUniqueId(), player.getName());
            var primary = plugin.donates().primaryActive(player.getUniqueId());
            String name = player.getName();
            var line = chat.formatLine(profile, primary, name, global, text);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                if (global) {
                    chat.broadcastGlobal(line);
                } else {
                    chat.broadcastLocal(player, line);
                }
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Чат: " + e.getMessage());
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> Msg.server(player, Msg.err("Ошибка чата.")));
        }
    }
}
