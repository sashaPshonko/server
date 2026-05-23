package dev.narek.pveauction.gui.clan;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.clan.ClanService;
import dev.narek.pveauction.model.ClanData;
import dev.narek.pveauction.model.ClanMember;
import dev.narek.pveauction.model.ClanPermissions;
import dev.narek.pveauction.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public final class ClanGuiListener implements Listener {

    private final PveAuctionPlugin plugin;
    private final ClanService clans;

    public ClanGuiListener(PveAuctionPlugin plugin, ClanService clans) {
        this.plugin = plugin;
        this.clans = clans;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        Object holder = top.getHolder();
        if (holder instanceof ClanMenu menu) {
            handleClanMenu(event, menu);
        } else if (holder instanceof ClanMemberPermMenu menu) {
            handlePermMenu(event, menu);
        } else if (holder instanceof ClanKickConfirmMenu menu) {
            handleKickConfirm(event, menu);
        } else if (holder instanceof ClanActionConfirmMenu menu) {
            handleActionConfirm(event, menu);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Object holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof ClanMenu
                || holder instanceof ClanMemberPermMenu
                || holder instanceof ClanKickConfirmMenu
                || holder instanceof ClanActionConfirmMenu) {
            event.setCancelled(true);
        }
    }

    private void handleClanMenu(InventoryClickEvent event, ClanMenu menu) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!player.getUniqueId().equals(menu.viewer().getUniqueId())) {
            return;
        }
        int raw = event.getRawSlot();
        if (raw >= menu.getInventory().getSize()) {
            return;
        }

        UUID memberId = menu.memberAt(raw);
        if (memberId != null) {
            if (!menu.viewerMember().isOwner()) {
                return;
            }
            if (memberId.equals(player.getUniqueId())) {
                return;
            }
            if (event.isShiftClick()) {
                toggleAllPermissions(player, memberId);
                return;
            }
            clans.runAsync(player, ok -> {}, () -> {
                ClanMember target = clans.repo().findMember(memberId).orElseThrow();
                if (target.isOwner()) {
                    throw new IllegalStateException("Нельзя менять права владельца.");
                }
                clans.runSync(player, () -> ClanMemberPermMenu.open(plugin, player, target));
            });
            return;
        }

        if (raw == ClanMenu.SLOT_INVITE) {
            player.closeInventory();
            Msg.clan(player, Msg.info("Напиши: /clan invite <ник>"));
        }
    }

    private void handlePermMenu(InventoryClickEvent event, ClanMemberPermMenu menu) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!player.getUniqueId().equals(menu.viewer().getUniqueId())) {
            return;
        }
        int raw = event.getRawSlot();
        if (raw >= menu.getInventory().getSize()) {
            return;
        }
        if (raw == ClanMemberPermMenu.SLOT_BACK) {
            openClanMenu(player);
            return;
        }
        if (raw == ClanMemberPermMenu.SLOT_INVITE) {
            menu.togglePerm(ClanPermissions.INVITE);
            savePerms(player, menu);
            return;
        }
        if (raw == ClanMemberPermMenu.SLOT_KICK) {
            menu.togglePerm(ClanPermissions.KICK);
            savePerms(player, menu);
            return;
        }
        if (raw == ClanMemberPermMenu.SLOT_WITHDRAW) {
            menu.togglePerm(ClanPermissions.WITHDRAW);
            savePerms(player, menu);
        }
    }

    private void toggleAllPermissions(Player player, UUID targetUuid) {
        clans.runAsync(player, ok -> {
            if (ok) {
                openClanMenu(player);
            }
        }, () -> {
            ClanMember viewer = clans.repo().findMember(player.getUniqueId()).orElseThrow();
            if (!viewer.isOwner()) {
                throw new IllegalStateException("Только владелец меняет права.");
            }
            ClanMember target = clans.repo().findMember(targetUuid).orElseThrow();
            if (target.isOwner()) {
                throw new IllegalStateException("Нельзя менять права владельца.");
            }
            int newMask = target.permissions() == 0 ? ClanPermissions.ALL : 0;
            clans.repo().setPermissions(targetUuid, newMask);
        });
    }

    private void savePerms(Player player, ClanMemberPermMenu menu) {
        clans.runAsync(player, ok -> {}, () -> {
            ClanMember viewer = clans.repo().findMember(player.getUniqueId()).orElseThrow();
            if (!viewer.isOwner()) {
                throw new IllegalStateException("Только владелец меняет права.");
            }
            clans.repo().setPermissions(menu.target().playerUuid(), menu.permissions());
        });
    }

    private void handleKickConfirm(InventoryClickEvent event, ClanKickConfirmMenu menu) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!player.getUniqueId().equals(menu.viewer().getUniqueId())) {
            return;
        }
        int raw = event.getRawSlot();
        if (raw == ClanKickConfirmMenu.SLOT_CANCEL) {
            player.closeInventory();
            openClanMenu(player);
            return;
        }
        if (raw != ClanKickConfirmMenu.SLOT_CONFIRM) {
            return;
        }
        UUID targetUuid = menu.targetUuid();
        if (targetUuid == null) {
            Msg.clan(player, Msg.err("Ошибка: игрок не найден."));
            return;
        }
        clans.runAsync(player, ok -> {
            if (ok) {
                Msg.clan(player, Msg.warn("Игрок исключён из клана."));
                Player kicked = Bukkit.getPlayer(targetUuid);
                if (kicked != null) {
                    Msg.clan(kicked, Msg.err("Тебя исключили из клана."));
                    plugin.scoreboardListener().refresh(kicked);
                }
                openClanMenu(player);
            }
        }, () -> {
            ClanMember actor = clans.repo().findMember(player.getUniqueId()).orElseThrow();
            if (!actor.can(ClanPermissions.KICK) && !actor.isOwner()) {
                throw new IllegalStateException("Нет права кикать.");
            }
            ClanMember target = clans.repo().findMember(targetUuid).orElseThrow();
            if (target.clanId() != actor.clanId()) {
                throw new IllegalStateException("Игрок не в твоём клане.");
            }
            if (target.isOwner()) {
                throw new IllegalStateException("Нельзя кикнуть владельца.");
            }
            int clanId = actor.clanId();
            String targetName = target.playerName();
            clans.repo().removeMember(targetUuid);
            clans.notifyClan(clanId, targetUuid,
                    Msg.warn(targetName + " исключён из клана."));
        });
    }

    private void handleActionConfirm(InventoryClickEvent event, ClanActionConfirmMenu menu) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!player.getUniqueId().equals(menu.viewer().getUniqueId())) {
            return;
        }
        int raw = event.getRawSlot();
        if (raw == ClanActionConfirmMenu.SLOT_CANCEL) {
            player.closeInventory();
            if (menu.action() != ClanActionConfirmMenu.Action.LEAVE) {
                openClanMenu(player);
            }
            return;
        }
        if (raw != ClanActionConfirmMenu.SLOT_CONFIRM) {
            return;
        }

        switch (menu.action()) {
            case LEAVE -> confirmLeave(player);
            case DISBAND -> confirmDisband(player);
            case SET_HOME -> confirmSetHome(player);
            case DEL_HOME -> confirmDelHome(player);
        }
    }

    private void confirmLeave(Player player) {
        clans.runAsync(player, ok -> {
            if (ok) {
                player.closeInventory();
                Msg.clan(player, Msg.warn("Ты покинул клан."));
                plugin.scoreboardListener().refresh(player);
            }
        }, () -> {
            ClanMember member = clans.repo().findMember(player.getUniqueId()).orElseThrow();
            if (member.isOwner()) {
                throw new IllegalStateException("Владелец не может выйти — используй /clan disband.");
            }
            int clanId = member.clanId();
            String name = player.getName();
            clans.repo().removeMember(player.getUniqueId());
            clans.notifyClan(clanId, player.getUniqueId(), Msg.warn(name + " покинул клан."));
        });
    }

    private void confirmDisband(Player player) {
        clans.runAsync(player, ok -> {
            if (ok) {
                player.closeInventory();
                Msg.clan(player, Msg.warn("Клан расформирован."));
                plugin.scoreboardListener().refresh(player);
            }
        }, () -> {
            ClanMember member = clans.repo().findMember(player.getUniqueId()).orElseThrow();
            if (!member.isOwner()) {
                throw new IllegalStateException("Только владелец может расформировать клан.");
            }
            ClanData clan = clans.repo().findClan(member.clanId()).orElseThrow();
            String clanName = clan.name();
            int clanId = member.clanId();
            String ownerName = player.getName();
            List<UUID> former = clans.repo().disbandClan(clanId);
            Component notice = Msg.err(ownerName + " расформировал клан «" + clanName + "».");
            clans.runSync(player, () -> {
                for (UUID uuid : former) {
                    Player online = Bukkit.getPlayer(uuid);
                    if (online != null) {
                        Msg.clan(online, notice);
                        plugin.scoreboardListener().refresh(online);
                    }
                }
            });
        });
    }

    private void confirmSetHome(Player player) {
        clans.runAsync(player, ok -> {
            if (ok) {
                player.closeInventory();
                Msg.clan(player, Msg.ok("База клана установлена."));
            }
        }, () -> {
            ClanMember member = clans.repo().findMember(player.getUniqueId()).orElseThrow();
            if (!member.isOwner()) {
                throw new IllegalStateException("Только владелец может ставить базу клана.");
            }
            int clanId = member.clanId();
            String name = player.getName();
            clans.repo().setClanHome(clanId, player.getLocation());
            clans.notifyClan(clanId, player.getUniqueId(),
                    Msg.ok(name + " установил базу клана."));
        });
    }

    private void confirmDelHome(Player player) {
        clans.runAsync(player, ok -> {
            if (ok) {
                player.closeInventory();
                Msg.clan(player, Msg.ok("База клана удалена."));
                openClanMenu(player);
            }
        }, () -> {
            ClanMember member = clans.repo().findMember(player.getUniqueId()).orElseThrow();
            if (!member.isOwner()) {
                throw new IllegalStateException("Только владелец может удалить базу клана.");
            }
            ClanData clan = clans.repo().findClan(member.clanId()).orElseThrow();
            if (!clan.hasHome()) {
                throw new IllegalStateException("База клана не установлена.");
            }
            int clanId = member.clanId();
            String name = player.getName();
            clans.repo().clearClanHome(clanId);
            clans.notifyClan(clanId, player.getUniqueId(),
                    Msg.warn(name + " удалил базу клана."));
        });
    }

    private void openClanMenu(Player player) {
        clans.runAsync(player, ok -> {}, () -> {
            ClanMember member = clans.repo().findMember(player.getUniqueId()).orElseThrow();
            ClanData clan = clans.repo().findClan(member.clanId()).orElseThrow();
            List<ClanMember> members = clans.repo().listMembers(member.clanId());
            clans.runSync(player, () -> ClanMenu.open(plugin, player, clan, member, members));
        });
    }
}
