package dev.narek.pveauction.listener;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.gui.trader.TraderShopMenu;
import dev.narek.pveauction.trader.TraderNpcService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class TraderNpcListener implements Listener {

    private final PveAuctionPlugin plugin;
    private final TraderNpcService trader;

    public TraderNpcListener(PveAuctionPlugin plugin, TraderNpcService trader) {
        this.plugin = plugin;
        this.trader = trader;
    }

    private void openShop(Player player) {
        TraderShopMenu.open(plugin, player);
    }

    /** ПКМ по жителю */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!trader.isTrader(event.getRightClicked())) {
            return;
        }
        event.setCancelled(true);
        openShop(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractAt(PlayerInteractAtEntityEvent event) {
        if (!trader.isTrader(event.getRightClicked())) {
            return;
        }
        event.setCancelled(true);
        openShop(event.getPlayer());
    }

    /** ЛКМ (удар) — invulnerable мешал; урон отменяем, магазин открываем */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!trader.isTrader(event.getEntity())) {
            return;
        }
        event.setCancelled(true);
        openShop(player);
    }

}
