package dev.narek.pveauction.listener;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.lockpick.armor.ArmorLockpickInteract;
import dev.narek.pveauction.lockpick.armor.ArmorLockpickService;
import dev.narek.pveauction.model.AdminRegion;
import dev.narek.pveauction.region.AdminRegionService;
import dev.narek.pveauction.util.Msg;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;

public final class AdminRegionProtectListener implements Listener {

    private final PveAuctionPlugin plugin;
    private final AdminRegionService regions;
    private final ArmorLockpickService armorLockpick;

    public AdminRegionProtectListener(PveAuctionPlugin plugin, AdminRegionService regions) {
        this(plugin, regions, new ArmorLockpickService(plugin));
    }

    public AdminRegionProtectListener(
            PveAuctionPlugin plugin,
            AdminRegionService regions,
            ArmorLockpickService armorLockpick
    ) {
        this.plugin = plugin;
        this.regions = regions;
        this.armorLockpick = armorLockpick;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (deny(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (deny(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) {
            block = ArmorLockpickInteract.resolveSmithingBlock(event);
        }
        if (block == null) {
            return;
        }
        if (regions.isWand(event.getItem())) {
            return;
        }
        if (ArmorLockpickInteract.isSmithingTableAttempt(event, plugin, armorLockpick)) {
            return;
        }
        if (denyInteract(event.getPlayer(), block)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (deny(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (deny(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> isProtectedBlock(block));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> isProtectedBlock(block));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player player)) {
            if (isProtectedBlock(event.getEntity().getLocation().getBlock())) {
                event.setCancelled(true);
            }
            return;
        }
        if (deny(player, event.getEntity().getLocation().getBlock())) {
            event.setCancelled(true);
        }
    }

    private boolean deny(Player player, Block block) {
        if (!regions.enabled() || regions.canBuildInRegions(player)) {
            return false;
        }
        try {
            if (regions.regionAt(block.getLocation()).isEmpty()) {
                return false;
            }
            Msg.server(player, Msg.err("Зона администрации — только для админов."));
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean denyInteract(Player player, Block block) {
        if (armorLockpick.isSpawnSmithing(block)
                && ArmorLockpickInteract.holdingArmorKey(player, plugin)) {
            return false;
        }
        if (!regions.enabled() || regions.canBuildInRegions(player)) {
            return false;
        }
        try {
            var regionOpt = regions.regionAt(block.getLocation());
            if (regionOpt.isEmpty()) {
                return false;
            }
            AdminRegion region = regionOpt.get();
            if (region.allowMemberInteract() && regions.isMemberInteractBlock(block)) {
                return false;
            }
            Msg.server(player, Msg.err("Зона администрации — только для админов."));
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean isProtectedBlock(Block block) {
        try {
            return regions.isProtected(block.getLocation());
        } catch (SQLException e) {
            return false;
        }
    }
}
