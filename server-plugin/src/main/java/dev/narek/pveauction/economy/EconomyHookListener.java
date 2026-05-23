package dev.narek.pveauction.economy;

import dev.narek.pveauction.PveAuctionPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;

public final class EconomyHookListener implements Listener {

    private final PveAuctionPlugin plugin;

    public EconomyHookListener(PveAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        String name = event.getPlugin().getName();
        if ("Vault".equals(name) || "Essentials".equals(name) || "EssentialsX".equals(name)) {
            plugin.retryEconomyHook();
        }
    }
}
