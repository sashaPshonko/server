package dev.narek.pveauction.listener;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.item.CustomItems;
import dev.narek.pveauction.item.StorageKeyType;
import dev.narek.pveauction.util.Msg;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class StorageKeyListener implements Listener {

    private final PveAuctionPlugin plugin;

    public StorageKeyListener(PveAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack hand = event.getItem();
        StorageKeyType type = CustomItems.keyType(plugin, hand);
        if (type == null) {
            return;
        }
        event.setCancelled(true);
        Msg.server(event.getPlayer(), Msg.info("Отмычка: ")
                .append(type.displayName())
                .append(Msg.info(" — хранилище подключим у сундука на спавне.")));
    }
}
