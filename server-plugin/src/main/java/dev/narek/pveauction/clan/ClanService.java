package dev.narek.pveauction.clan;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.chat.ChatService;
import dev.narek.pveauction.db.ClanRepository;
import dev.narek.pveauction.model.ClanData;
import dev.narek.pveauction.model.PlayerProfile;
import dev.narek.pveauction.model.ClanMember;
import dev.narek.pveauction.model.ClanPermissions;
import dev.narek.pveauction.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class ClanService {

    private final PveAuctionPlugin plugin;
    private final ClanRepository clans;

    public ClanService(PveAuctionPlugin plugin, ClanRepository clans) {
        this.plugin = plugin;
        this.clans = clans;
    }

    public void runAsync(Player player, Consumer<Boolean> onDone, ClanTask task) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                task.run();
                runSync(player, () -> onDone.accept(true));
            } catch (SQLException e) {
                plugin.getLogger().severe(e.getMessage());
                runSync(player, () -> {
                    Msg.clan(player, Msg.err("Ошибка базы данных."));
                    onDone.accept(false);
                });
            } catch (IllegalStateException e) {
                runSync(player, () -> {
                    Msg.clan(player, Msg.err(e.getMessage()));
                    onDone.accept(false);
                });
            }
        });
    }

    public void notifyClan(int clanId, @Nullable UUID exclude, Component body) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                for (ClanMember m : clans.listMembers(clanId)) {
                    if (exclude != null && m.playerUuid().equals(exclude)) {
                        continue;
                    }
                    Player online = Bukkit.getPlayer(m.playerUuid());
                    if (online != null) {
                        Msg.clan(online, body);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe(e.getMessage());
            }
        });
    }

    /** Сообщение всем онлайн в клане, включая инициатора. */
    public void notifyClanAll(int clanId, Component body) {
        notifyClan(clanId, null, body);
    }

    public static Component memberAction(String playerName, String actionText, long amount, boolean warn) {
        Component action = warn
                ? Msg.warn(actionText)
                : Msg.info(actionText);
        return Component.text(playerName, NamedTextColor.WHITE, TextDecoration.BOLD)
                .append(action)
                .append(Msg.money(amount));
    }

    public void refreshClanOnline(int clanId) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                for (ClanMember m : clans.listMembers(clanId)) {
                    Player online = Bukkit.getPlayer(m.playerUuid());
                    if (online != null) {
                        plugin.scoreboardListener().refresh(online);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe(e.getMessage());
            }
        });
    }

    public void sendClanChat(Player sender, ClanMember member, ClanData clan, String text) {
        try {
            Component body = plugin.chat().formatClanChatBody(
                    new ChatService.ClanMemberParts(sender.getName(), member.isOwner()),
                    text);
            for (ClanMember m : clans.listMembers(clan.id())) {
                Player online = Bukkit.getPlayer(m.playerUuid());
                if (online != null) {
                    Msg.clan(online, body);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe(e.getMessage());
            Msg.clan(sender, Msg.err("Ошибка чата клана."));
        }
    }

    public void runSync(Player player, Runnable task) {
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    public void sendInviteMessage(Player target, String clanName, String inviterName) {
        Component accept = Component.text("[Принять]", NamedTextColor.GREEN, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/clan accept"))
                .hoverEvent(HoverEvent.showText(Component.text("Нажми, чтобы вступить в клан", NamedTextColor.GRAY)));
        Component deny = Component.text(" [Отклонить]", NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/clan deny"))
                .hoverEvent(HoverEvent.showText(Component.text("Отклонить приглашение", NamedTextColor.GRAY)));
        target.sendMessage(Component.empty());
        target.sendMessage(Component.text("Приглашение в клан ", NamedTextColor.GRAY)
                .append(Component.text(clanName, NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" от ", NamedTextColor.GRAY))
                .append(Component.text(inviterName, NamedTextColor.AQUA)));
        target.sendMessage(Component.text("  ").append(accept).append(deny));
    }

    public static String permLabel(ClanMember member) {
        if (member.isOwner()) {
            return "все права";
        }
        return permLabel(member.permissions());
    }

    public static String permLabel(int mask) {
        StringBuilder sb = new StringBuilder();
        if (ClanPermissions.has(mask, ClanPermissions.INVITE)) {
            sb.append("приглашать ");
        }
        if (ClanPermissions.has(mask, ClanPermissions.KICK)) {
            sb.append("кикать ");
        }
        if (ClanPermissions.has(mask, ClanPermissions.WITHDRAW)) {
            sb.append("снимать ");
        }
        if (sb.isEmpty()) {
            return "нет прав";
        }
        return sb.toString().trim();
    }

    public ClanRepository repo() {
        return clans;
    }

    public Player findOnline(String name) {
        return Bukkit.getPlayerExact(name);
    }

    public void ensureProfile(UUID uuid, String name) throws SQLException {
        plugin.players().getOrCreate(uuid, name);
    }

    @FunctionalInterface
    public interface ClanTask {
        void run() throws SQLException;
    }
}
