package dev.narek.pveauction.listener;

import dev.narek.pveauction.travel.TeleportRequestService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class TpRequestListener implements Listener {

    private final TeleportRequestService tp;

    public TpRequestListener(TeleportRequestService tp) {
        this.tp = tp;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tp.onQuit(event.getPlayer().getUniqueId());
    }
}
