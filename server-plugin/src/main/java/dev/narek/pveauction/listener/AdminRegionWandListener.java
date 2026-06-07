package dev.narek.pveauction.listener;

import dev.narek.pveauction.region.AdminRegionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class AdminRegionWandListener implements Listener {

    private final AdminRegionService regions;

    public AdminRegionWandListener(AdminRegionService regions) {
        this.regions = regions;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWand(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!regions.enabled()) {
            return;
        }
        var player = event.getPlayer();
        if (!regions.canManage(player)) {
            return;
        }
        if (!regions.isWand(event.getItem())) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }

        event.setCancelled(true);
        var loc = event.getClickedBlock().getLocation();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            regions.setPos1(player, loc);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            regions.setPos2(player, loc);
        } else {
            return;
        }
    }
}
