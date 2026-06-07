package dev.narek.pveauction.listener;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.item.CustomItems;
import dev.narek.pveauction.item.StorageKeyType;
import dev.narek.pveauction.lockpick.armor.ArmorLockpickInteract;
import dev.narek.pveauction.lockpick.armor.ArmorLockpickService;
import dev.narek.pveauction.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class StorageKeyListener implements Listener {

    private final PveAuctionPlugin plugin;
    private final ArmorLockpickService armorLockpick;

    public StorageKeyListener(PveAuctionPlugin plugin) {
        this(plugin, new ArmorLockpickService(plugin));
    }

    public StorageKeyListener(PveAuctionPlugin plugin, ArmorLockpickService armorLockpick) {
        this.plugin = plugin;
        this.armorLockpick = armorLockpick;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        boolean blockUse = action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK;
        if (action != Action.RIGHT_CLICK_AIR && !blockUse) {
            return;
        }
        ItemStack hand = event.getHand() == EquipmentSlot.HAND
                ? mainHand(event)
                : event.getItem();
        StorageKeyType type = CustomItems.keyType(plugin, hand);
        if (type == null) {
            return;
        }
        if (type == StorageKeyType.ARMOR
                && ArmorLockpickInteract.isSmithingTableAttempt(event, plugin, armorLockpick)) {
            return;
        }
        event.setCancelled(true);
        if (type == StorageKeyType.ARMOR) {
            Msg.server(event.getPlayer(), Msg.info("Отмычка к броне: ЛКМ или ПКМ по ")
                    .append(Component.text("кузнечному столу", NamedTextColor.GOLD))
                    .append(Msg.info(" на спавне.")));
            return;
        }
        Msg.server(event.getPlayer(), Msg.info("Отмычка: ")
                .append(type.displayName())
                .append(Msg.info(" — хранилище подключим у сундука на спавне.")));
    }

    private static ItemStack mainHand(PlayerInteractEvent event) {
        ItemStack fromEvent = event.getItem();
        if (fromEvent != null && !fromEvent.getType().isAir()) {
            return fromEvent;
        }
        return event.getPlayer().getInventory().getItemInMainHand();
    }
}
