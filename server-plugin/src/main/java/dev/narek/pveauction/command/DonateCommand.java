package dev.narek.pveauction.command;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.donate.DonateService;
import dev.narek.pveauction.donate.DonateType;
import dev.narek.pveauction.model.PlayerDonate;
import dev.narek.pveauction.util.Msg;
import dev.narek.pveauction.util.RankColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class DonateCommand implements CommandExecutor, TabCompleter {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final PveAuctionPlugin plugin;
    private final DonateService donates;

    public DonateCommand(PveAuctionPlugin plugin, DonateService donates) {
        this.plugin = plugin;
        this.donates = donates;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission("pveauction.donate.admin")) {
            Msg.donate(sender, Msg.err("Нет прав."));
            return true;
        }
        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "give" -> give(sender, args);
            case "list" -> list(sender, args);
            case "remove", "take" -> remove(sender, args);
            case "types" -> types(sender);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    private boolean give(CommandSender sender, String[] args) {
        if (args.length < 4) {
            Msg.donate(sender, Msg.err("Использование: /donate give <ник> <донат> <дней|forever>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            Msg.donate(sender, Msg.err("Игрок не в сети."));
            return true;
        }
        Optional<DonateType> type = DonateType.byId(args[2]);
        if (type.isEmpty()) {
            Msg.donate(sender, Msg.err("Неизвестный донат. Список: /donate types"));
            return true;
        }

        String grantedBy = sender instanceof Player p ? p.getName() : "console";
        UUID targetId = target.getUniqueId();
        String duration = args[3];

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                DonateService.GrantResult result = donates.grant(targetId, type.get(), duration, grantedBy);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!result.success()) {
                        Msg.donate(sender, Msg.err(result.error()));
                        return;
                    }
                    String when = result.expiresAt() == null
                            ? "навсегда"
                            : "до " + WHEN.format(Instant.ofEpochMilli(result.expiresAt()));
                    DonateType donate = type.get();
                    Component donateName = Msg.colored(donate.displayName(), donate.color());
                    Msg.donate(sender, Msg.ok("Донат ")
                            .append(donateName)
                            .append(Msg.ok(" → " + target.getName() + " (" + when + ")")));
                    Msg.server(target, Msg.ok("Выдан донат: ")
                            .append(donateName)
                            .append(Msg.ok(" (" + when + ")")));
                    donates.refreshOnlinePlayer(targetId);
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Донат give: " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> Msg.donate(sender, Msg.err("Ошибка базы данных.")));
            }
        });
        return true;
    }

    private boolean list(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Msg.donate(sender, Msg.err("Использование: /donate list <ник>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            Msg.donate(sender, Msg.err("Игрок не в сети."));
            return true;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<PlayerDonate> active = donates.active(target.getUniqueId());
                var primary = donates.primaryActive(target.getUniqueId());
                int maxLots = donates.maxActiveLots(target.getUniqueId());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Msg.donate(sender, Msg.info("Донаты " + target.getName() + " (лотов на АХ: " + maxLots + "):"));
                    if (active.isEmpty()) {
                        Msg.donate(sender, Msg.warn("  нет активных"));
                        return;
                    }
                    for (PlayerDonate d : active) {
                        String until = d.permanent()
                                ? "навсегда"
                                : "до " + WHEN.format(Instant.ofEpochMilli(d.expiresAt()));
                        boolean isPrimary = primary.isPresent()
                                && primary.get().donateId().equals(d.donateId());
                        Msg.donate(sender, Component.text("  • ", NamedTextColor.GRAY)
                                .append(Msg.colored(d.displayName(), d.color()))
                                .append(Component.text(" — " + until, NamedTextColor.GRAY))
                                .append(isPrimary
                                        ? Component.text(" ← действует", NamedTextColor.GREEN)
                                        : Component.empty()));
                    }
                });
            } catch (SQLException e) {
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> Msg.donate(sender, Msg.err("Ошибка базы данных.")));
            }
        });
        return true;
    }

    private boolean remove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Msg.donate(sender, Msg.err("Использование: /donate remove <ник> <донат>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            Msg.donate(sender, Msg.err("Игрок не в сети."));
            return true;
        }
        Optional<DonateType> type = DonateType.byId(args[2]);
        if (type.isEmpty()) {
            Msg.donate(sender, Msg.err("Неизвестный донат."));
            return true;
        }

        UUID targetId = target.getUniqueId();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                int n = donates.remove(targetId, type.get().id());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (n > 0) {
                        DonateType donate = type.get();
                        Component donateName = Msg.colored(donate.displayName(), donate.color());
                        Msg.donate(sender, Msg.ok("Снят донат ")
                                .append(donateName)
                                .append(Msg.ok(" с " + target.getName())));
                        Msg.server(target, Msg.warn("Донат снят: ").append(donateName));
                        donates.refreshOnlinePlayer(targetId);
                    } else {
                        Msg.donate(sender, Msg.err("У игрока нет такого доната."));
                    }
                });
            } catch (SQLException e) {
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> Msg.donate(sender, Msg.err("Ошибка базы данных.")));
            }
        });
        return true;
    }

    private boolean types(CommandSender sender) {
        Msg.donate(sender, Msg.info("Донаты (id — цвет):"));
        for (DonateType type : DonateType.values()) {
            Msg.donate(sender, Component.text("  ", NamedTextColor.GRAY)
                    .append(Component.text(type.id(), NamedTextColor.WHITE))
                    .append(Component.text(" → ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(type.displayName(), RankColors.parse(type.color()))));
        }
        return true;
    }

    private static void sendUsage(CommandSender sender) {
        sender.sendMessage("""
                /donate give <ник> <донат> <дней|forever>
                /donate list <ник>
                /donate remove <ник> <донат>
                /donate types""");
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission("pveauction.donate.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(List.of("give", "list", "remove", "types"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("give")
                || args[0].equalsIgnoreCase("list")
                || args[0].equalsIgnoreCase("remove"))) {
            return filterOnline(args[1]);
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("remove"))) {
            return filter(donateIds(), args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            return filter(List.of("7", "30", "forever", "navsegda"), args[3]);
        }
        return List.of();
    }

    private static List<String> donateIds() {
        List<String> ids = new ArrayList<>();
        for (DonateType type : DonateType.values()) {
            ids.add(type.id());
        }
        return ids;
    }

    private static List<String> filterOnline(String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            String name = online.getName();
            if (name != null && name.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(name);
            }
        }
        return out;
    }

    private static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(o);
            }
        }
        return out;
    }
}
