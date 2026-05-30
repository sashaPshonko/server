package dev.narek.pveauction.travel;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.util.Msg;
import dev.narek.pveauction.world.WorldTeleportService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleportRequestService {

    private final PveAuctionPlugin plugin;
    private final Map<UUID, Request> incomingByTarget = new ConcurrentHashMap<>();
    private final Map<UUID, Request> outgoingByRequester = new ConcurrentHashMap<>();

    public TeleportRequestService(PveAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    public void send(Player requester, Player target) {
        if (requester.getUniqueId().equals(target.getUniqueId())) {
            Msg.tp(requester, Msg.err("Нельзя телепортироваться к себе."));
            return;
        }

        clearOutgoing(requester.getUniqueId(), false);
        clearIncoming(target.getUniqueId(), false);

        long timeoutSec = plugin.getConfig().getLong("tp-request-timeout-seconds", 30L);
        long expiresAt = System.currentTimeMillis() + timeoutSec * 1000L;
        Request request = new Request(requester.getUniqueId(), target.getUniqueId(), expiresAt);

        incomingByTarget.put(target.getUniqueId(), request);
        outgoingByRequester.put(requester.getUniqueId(), request);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> expireIfPending(request),
                timeoutSec * 20L
        );
        request.timeoutTask = task;

        Msg.tp(requester, Msg.info("Запрос отправлен игроку ")
                .append(Msg.ok(target.getName()))
                .append(Msg.info(". Ожидание " + timeoutSec + " сек…")));

        target.sendMessage(buildIncomingLine(requester.getName()));
    }

    public void accept(Player target) {
        Request request = incomingByTarget.get(target.getUniqueId());
        if (request == null) {
            Msg.tp(target, Msg.err("Нет входящих запросов на телепорт."));
            return;
        }
        if (request.isExpired()) {
            clearRequest(request);
            Msg.tp(target, Msg.err("Запрос уже истёк."));
            return;
        }

        Player requester = Bukkit.getPlayer(request.requesterId);
        if (requester == null || !requester.isOnline()) {
            clearRequest(request);
            Msg.tp(target, Msg.err("Игрок уже не в сети."));
            return;
        }

        clearRequest(request);
        Msg.tp(target, Msg.ok("Телепорт принят: ") .append(Msg.info(requester.getName())));
        Msg.tp(requester, Msg.ok("Телепорт к ") .append(Msg.info(target.getName())));

        WorldTeleportService.teleport(plugin, requester, target.getLocation(), ok -> {
            if (!requester.isOnline()) {
                return;
            }
            if (!ok) {
                Msg.tp(requester, Msg.err("Не удалось телепортироваться."));
            }
        });
    }

    public void deny(Player target) {
        Request request = incomingByTarget.get(target.getUniqueId());
        if (request == null) {
            Msg.tp(target, Msg.err("Нет входящих запросов на телепорт."));
            return;
        }

        Player requester = Bukkit.getPlayer(request.requesterId);
        clearRequest(request);

        Msg.tp(target, Msg.warn("Запрос отклонён."));
        if (requester != null && requester.isOnline()) {
            Msg.tp(requester, Msg.err(target.getName() + " отклонил телепорт."));
        }
    }

    public void onQuit(UUID playerId) {
        clearOutgoing(playerId, true);
        Request incoming = incomingByTarget.remove(playerId);
        if (incoming != null) {
            incoming.cancelTask();
            outgoingByRequester.remove(incoming.requesterId);
            Player requester = Bukkit.getPlayer(incoming.requesterId);
            if (requester != null && requester.isOnline()) {
                Msg.tp(requester, Msg.err("Игрок вышел — запрос отменён."));
            }
        }
    }

    private void expireIfPending(Request request) {
        Request current = incomingByTarget.get(request.targetId);
        if (current != request || request.isExpired()) {
            return;
        }

        Player requester = Bukkit.getPlayer(request.requesterId);
        Player target = Bukkit.getPlayer(request.targetId);
        clearRequest(request);

        if (requester != null && requester.isOnline()) {
            Msg.tp(requester, Msg.err("Время вышло — запрос отменён."));
        }
        if (target != null && target.isOnline()) {
            Msg.tp(target, Msg.warn("Запрос на телепорт истёк."));
        }
    }

    private void clearOutgoing(UUID requesterId, boolean notifyTarget) {
        Request old = outgoingByRequester.remove(requesterId);
        if (old == null) {
            return;
        }
        old.cancelTask();
        incomingByTarget.remove(old.targetId, old);
        if (notifyTarget) {
            Player target = Bukkit.getPlayer(old.targetId);
            if (target != null && target.isOnline()) {
                Msg.tp(target, Msg.warn("Запрос на телепорт отменён."));
            }
        }
    }

    private void clearIncoming(UUID targetId, boolean notifyRequester) {
        Request old = incomingByTarget.remove(targetId);
        if (old == null) {
            return;
        }
        old.cancelTask();
        outgoingByRequester.remove(old.requesterId, old);
        if (notifyRequester) {
            Player requester = Bukkit.getPlayer(old.requesterId);
            if (requester != null && requester.isOnline()) {
                Msg.tp(requester, Msg.warn("Предыдущий запрос заменён новым."));
            }
        }
    }

    private void clearRequest(Request request) {
        request.cancelTask();
        incomingByTarget.remove(request.targetId, request);
        outgoingByRequester.remove(request.requesterId, request);
    }

    private Component buildIncomingLine(String requesterName) {
        Component accept = Component.text(" [✔ Принять]", NamedTextColor.GREEN, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/tpaccept"))
                .hoverEvent(HoverEvent.showText(Component.text("Принять телепорт", NamedTextColor.GREEN)));
        Component deny = Component.text(" [✖ Отклонить]", NamedTextColor.RED, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/tpdeny"))
                .hoverEvent(HoverEvent.showText(Component.text("Отклонить", NamedTextColor.RED)));

        return Msg.prefixTp()
                .append(Msg.info(requesterName + " хочет телепортироваться к тебе."))
                .append(accept)
                .append(deny);
    }

    private static final class Request {
        final UUID requesterId;
        final UUID targetId;
        final long expiresAt;
        BukkitTask timeoutTask;

        Request(UUID requesterId, UUID targetId, long expiresAt) {
            this.requesterId = requesterId;
            this.targetId = targetId;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }

        void cancelTask() {
            if (timeoutTask != null) {
                timeoutTask.cancel();
                timeoutTask = null;
            }
        }
    }
}
