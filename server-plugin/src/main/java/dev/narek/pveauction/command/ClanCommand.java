package dev.narek.pveauction.command;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.clan.ClanService;
import dev.narek.pveauction.db.ClanRepository;
import dev.narek.pveauction.gui.clan.ClanActionConfirmMenu;
import dev.narek.pveauction.gui.clan.ClanKickConfirmMenu;
import dev.narek.pveauction.gui.clan.ClanMenu;
import dev.narek.pveauction.model.ClanData;
import dev.narek.pveauction.model.ClanMember;
import dev.narek.pveauction.model.ClanPermissions;
import dev.narek.pveauction.model.ClanRole;
import dev.narek.pveauction.util.GuiItems;
import dev.narek.pveauction.util.Msg;
import dev.narek.pveauction.world.WorldTeleportService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class ClanCommand implements CommandExecutor, TabCompleter {

    private final PveAuctionPlugin plugin;
    private final ClanService clans;

    public ClanCommand(PveAuctionPlugin plugin, ClanService clans) {
        this.plugin = plugin;
        this.clans = clans;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }

        if (args.length == 0) {
            Msg.send(player, Msg.info("Команды: create, menu, invite, accept, deny, kick, leave, disband, money, invest, withdraw, sethome, delhome, home"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "create" -> handleCreate(player, args);
            case "menu" -> handleMenu(player);
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player);
            case "deny" -> handleDeny(player);
            case "kick" -> handleKick(player, args);
            case "money" -> handleMoney(player);
            case "invest", "deposit" -> handleInvest(player, args);
            case "withdraw", "take" -> handleWithdraw(player, args);
            case "sethome" -> handleSetHome(player);
            case "delhome" -> handleDelHome(player);
            case "home" -> handleHome(player);
            case "leave" -> handleLeave(player);
            case "disband" -> handleDisband(player);
            default -> {
                Msg.send(player, Msg.err("Неизвестная подкоманда."));
                yield true;
            }
        };
    }

    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            Msg.send(player, Msg.err("Использование: /clan create <название>"));
            return true;
        }
        String name = args[1].trim();
        if (name.length() < 3 || name.length() > 16) {
            Msg.send(player, Msg.err("Название: 3–16 символов."));
            return true;
        }
        clans.runAsync(player, ok -> {
            if (ok) {
                Msg.send(player, Msg.ok("Клан «" + name + "» создан."));
                plugin.scoreboardListener().refresh(player);
            }
        }, () -> {
            if (clans.repo().findMember(player.getUniqueId()).isPresent()) {
                throw new IllegalStateException("Ты уже в клане.");
            }
            if (clans.repo().findClanByName(name).isPresent()) {
                throw new IllegalStateException("Такой клан уже есть.");
            }
            clans.ensureProfile(player.getUniqueId(), player.getName());
            clans.repo().createClan(name, player.getUniqueId());
        });
        return true;
    }

    private boolean handleMenu(Player player) {
        clans.runAsync(player, ok -> {}, () -> {
            ClanMember member = clans.repo().findMember(player.getUniqueId())
                    .orElseThrow(() -> new IllegalStateException("Ты не в клане."));
            ClanData clan = clans.repo().findClan(member.clanId())
                    .orElseThrow(() -> new IllegalStateException("Клан не найден."));
            var members = clans.repo().listMembers(member.clanId());
            clans.runSync(player, () -> ClanMenu.open(plugin, player, clan, member, members));
        });
        return true;
    }

    private boolean handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            Msg.send(player, Msg.err("Использование: /clan invite <ник>"));
            return true;
        }
        String targetName = args[1];
        clans.runAsync(player, ok -> {
            if (ok) {
                Msg.send(player, Msg.ok("Приглашение отправлено."));
            }
        }, () -> {
            ClanMember member = requireMember(player);
            if (!member.can(ClanPermissions.INVITE)) {
                throw new IllegalStateException("Нет права приглашать.");
            }
            Player target = clans.findOnline(targetName);
            if (target == null) {
                throw new IllegalStateException("Игрок должен быть онлайн.");
            }
            if (clans.repo().findMember(target.getUniqueId()).isPresent()) {
                throw new IllegalStateException("Игрок уже в клане.");
            }
            ClanData clan = clans.repo().findClan(member.clanId()).orElseThrow();
            clans.repo().deleteInvitesForTarget(target.getUniqueId());
            long ttl = plugin.getConfig().getLong("clan.invite-ttl-seconds", 120) * 1000L;
            clans.repo().createInvite(clan.id(), player.getUniqueId(), target.getUniqueId(), ttl);
            clans.runSync(target, () -> clans.sendInviteMessage(target, clan.name(), player.getName()));
        });
        return true;
    }

    private boolean handleAccept(Player player) {
        clans.runAsync(player, ok -> {
            if (ok) {
                Msg.send(player, Msg.ok("Ты вступил в клан."));
                plugin.scoreboardListener().refresh(player);
            }
        }, () -> {
            if (clans.repo().findMember(player.getUniqueId()).isPresent()) {
                throw new IllegalStateException("Ты уже в клане.");
            }
            ClanRepository.InviteRow invite = clans.repo().findActiveInviteForTarget(player.getUniqueId())
                    .orElseThrow(() -> new IllegalStateException("Нет активного приглашения."));
            clans.ensureProfile(player.getUniqueId(), player.getName());
            clans.repo().addMember(invite.clanId(), player.getUniqueId(), ClanRole.MEMBER, 0);
            clans.repo().deleteInvite(invite.id());
            clans.repo().deleteInvitesForTarget(player.getUniqueId());
        });
        return true;
    }

    private boolean handleDeny(Player player) {
        clans.runAsync(player, ok -> {
            if (ok) {
                Msg.send(player, Msg.warn("Приглашение отклонено."));
            }
        }, () -> clans.repo().deleteInvitesForTarget(player.getUniqueId()));
        return true;
    }

    private boolean handleKick(Player player, String[] args) {
        if (args.length < 2) {
            Msg.send(player, Msg.err("Использование: /clan kick <ник>"));
            return true;
        }
        String targetName = args[1];
        clans.runAsync(player, ok -> {}, () -> {
            ClanMember actor = requireMember(player);
            if (!actor.can(ClanPermissions.KICK)) {
                throw new IllegalStateException("Нет права кикать.");
            }
            ClanMember target = findMemberByName(actor.clanId(), targetName);
            if (target.isOwner()) {
                throw new IllegalStateException("Нельзя кикнуть владельца.");
            }
            if (target.playerUuid().equals(player.getUniqueId())) {
                throw new IllegalStateException("Нельзя кикнуть себя.");
            }
            clans.runSync(player, () -> ClanKickConfirmMenu.open(plugin, player, targetName, target.playerUuid()));
        });
        return true;
    }

    private boolean handleMoney(Player player) {
        clans.runAsync(player, ok -> {}, () -> {
            ClanMember member = requireMember(player);
            ClanData clan = clans.repo().findClan(member.clanId()).orElseThrow();
            long balance = clan.balance();
            clans.runSync(player, () -> Msg.send(player, Msg.info("Казна клана «" + clan.name() + "»: ")
                    .append(Msg.money(balance))));
        });
        return true;
    }

    private boolean handleInvest(Player player, String[] args) {
        if (!plugin.economy().isEnabled()) {
            Msg.send(player, Msg.err("Экономика не подключена."));
            return true;
        }
        if (args.length < 2) {
            Msg.send(player, Msg.err("Использование: /clan invest <сумма>"));
            return true;
        }
        long amount = parseAmount(player, args[1]);
        clans.runAsync(player, ok -> {
            if (ok) {
                Msg.send(player, Msg.ok("Вложено в клан ").append(Msg.money(amount)));
            }
        }, () -> {
            ClanMember member = requireMember(player);
            if (!plugin.economy().has(player, amount)) {
                throw new IllegalStateException("Не хватает денег.");
            }
            if (!plugin.economy().withdraw(player, amount)) {
                throw new IllegalStateException("Не удалось списать.");
            }
            clans.repo().addBalance(member.clanId(), amount);
        });
        return true;
    }

    private boolean handleWithdraw(Player player, String[] args) {
        if (!plugin.economy().isEnabled()) {
            Msg.send(player, Msg.err("Экономика не подключена."));
            return true;
        }
        if (args.length < 2) {
            Msg.send(player, Msg.err("Использование: /clan withdraw <сумма>"));
            return true;
        }
        long amount = parseAmount(player, args[1]);
        clans.runAsync(player, ok -> {
            if (ok) {
                Msg.send(player, Msg.ok("Снято из клана ").append(Msg.money(amount)));
            }
        }, () -> {
            ClanMember member = requireMember(player);
            if (!member.can(ClanPermissions.WITHDRAW)) {
                throw new IllegalStateException("Нет права снимать деньги.");
            }
            if (!clans.repo().withdrawBalance(member.clanId(), amount)) {
                throw new IllegalStateException("В казне недостаточно средств.");
            }
            if (!plugin.economy().deposit(player, amount)) {
                clans.repo().addBalance(member.clanId(), amount);
                throw new IllegalStateException("Не удалось выдать деньги.");
            }
        });
        return true;
    }

    private boolean handleLeave(Player player) {
        clans.runAsync(player, ok -> {}, () -> {
            requireMember(player);
            clans.runSync(player, () -> ClanActionConfirmMenu.open(player, ClanActionConfirmMenu.Action.LEAVE));
        });
        return true;
    }

    private boolean handleDisband(Player player) {
        clans.runAsync(player, ok -> {}, () -> {
            ClanMember member = requireMember(player);
            if (!member.isOwner()) {
                throw new IllegalStateException("Только владелец может расформировать клан.");
            }
            clans.runSync(player, () -> ClanActionConfirmMenu.open(player, ClanActionConfirmMenu.Action.DISBAND));
        });
        return true;
    }

    private boolean handleDelHome(Player player) {
        clans.runAsync(player, ok -> {}, () -> {
            ClanMember member = requireMember(player);
            if (!member.isOwner()) {
                throw new IllegalStateException("Только владелец может удалить клановый дом.");
            }
            ClanData clan = clans.repo().findClan(member.clanId()).orElseThrow();
            if (!clan.hasHome()) {
                throw new IllegalStateException("Клановый дом не установлен.");
            }
            clans.runSync(player, () -> ClanActionConfirmMenu.open(player, ClanActionConfirmMenu.Action.DEL_HOME));
        });
        return true;
    }

    private boolean handleSetHome(Player player) {
        clans.runAsync(player, ok -> {
            if (ok) {
                Msg.send(player, Msg.ok("Клановый дом установлен."));
            }
        }, () -> {
            ClanMember member = requireMember(player);
            if (!member.isOwner()) {
                throw new IllegalStateException("Только владелец может ставить клановый дом.");
            }
            clans.repo().setClanHome(member.clanId(), player.getLocation());
        });
        return true;
    }

    private boolean handleHome(Player player) {
        clans.runAsync(player, ok -> {}, () -> {
            ClanMember member = requireMember(player);
            ClanData clan = clans.repo().findClan(member.clanId()).orElseThrow();
            if (!clan.hasHome()) {
                throw new IllegalStateException("Клановый дом не установлен.");
            }
            var loc = clan.homeLocation();
            if (loc == null) {
                throw new IllegalStateException("Мир кланового дома не загружен.");
            }
            clans.runSync(player, () -> WorldTeleportService.teleport(plugin, player, loc, success -> {
                if (success) {
                    Msg.send(player, Msg.ok("Телепорт в клановый дом."));
                } else {
                    Msg.send(player, Msg.err("Не удалось телепортироваться."));
                }
            }));
        });
        return true;
    }

    private ClanMember requireMember(Player player) throws java.sql.SQLException {
        return clans.repo().findMember(player.getUniqueId())
                .orElseThrow(() -> new IllegalStateException("Ты не в клане."));
    }

    private ClanMember findMemberByName(int clanId, String name) throws java.sql.SQLException {
        for (ClanMember m : clans.repo().listMembers(clanId)) {
            if (m.playerName().equalsIgnoreCase(name)) {
                return m;
            }
        }
        throw new IllegalStateException("Участник не найден в клане.");
    }

    private long parseAmount(Player player, String raw) {
        try {
            long amount = ClanService.parseAmount(raw);
            ClanService.validateAmount(amount);
            return amount;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Неверная сумма.");
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            return filter(args[0],
                    "create", "menu", "invite", "accept", "deny", "kick", "leave", "disband",
                    "money", "invest", "withdraw", "sethome", "delhome", "home");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick"))) {
            List<String> out = new ArrayList<>();
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(p.getName());
                }
            }
            return out;
        }
        return List.of();
    }

    private static List<String> filter(String prefix, String... options) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.startsWith(p)) {
                out.add(o);
            }
        }
        return out;
    }
}
