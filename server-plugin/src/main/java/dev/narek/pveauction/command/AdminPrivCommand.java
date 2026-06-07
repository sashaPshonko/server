package dev.narek.pveauction.command;

import dev.narek.pveauction.model.AdminRegion;
import dev.narek.pveauction.region.AdminRegionService;
import dev.narek.pveauction.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AdminPrivCommand implements CommandExecutor, TabCompleter {

    private final AdminRegionService regions;

    public AdminPrivCommand(AdminRegionService regions) {
        this.regions = regions;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }
        if (!regions.canManage(player)) {
            Msg.server(player, Msg.err("Нет доступа. Нужно pveauction.apriv"));
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        try {
            switch (sub) {
                case "wand", "топор" -> regions.giveWand(player);
                case "delete", "удалить" -> {
                    if (args.length < 2) {
                        Msg.server(player, Msg.err("Использование: /apriv delete <имя>"));
                        return true;
                    }
                    boolean removed = regions.repo().deleteByName(args[1]);
                    if (removed) {
                        Msg.server(player, Msg.ok("Регион удалён."));
                    } else {
                        Msg.server(player, Msg.err("Регион не найден."));
                    }
                }
                case "list", "список" -> {
                    List<AdminRegion> all = regions.repo().listAll();
                    if (all.isEmpty()) {
                        Msg.server(player, Msg.info("Нет админ-приватов."));
                        return true;
                    }
                    Msg.server(player, Msg.info("Админ-приваты (" + all.size() + "):"));
                    for (AdminRegion r : all) {
                        player.sendMessage(Component.text(" • ", NamedTextColor.DARK_GRAY)
                                .append(Component.text(r.name(), NamedTextColor.GOLD))
                                .append(Component.text(" — " + r.world() + " ", NamedTextColor.GRAY))
                                .append(Component.text(
                                        "X " + r.minX() + ".." + r.maxX()
                                                + " Y " + r.minY() + ".." + r.maxY()
                                                + " Z " + r.minZ() + ".." + r.maxZ(),
                                        NamedTextColor.WHITE)));
                    }
                }
                case "info" -> {
                    var opt = regions.regionAt(player.getLocation());
                    if (opt.isEmpty()) {
                        Msg.server(player, Msg.info("Здесь нет админ-привата."));
                        return true;
                    }
                    AdminRegion r = opt.get();
                    Msg.server(player, Msg.ok("Регион «" + r.name() + "»")
                            .append(Component.text(" | " + r.world(), NamedTextColor.GRAY))
                            .append(Component.text(
                                    " X " + r.minX() + "…" + r.maxX()
                                            + " Y " + r.minY() + "…" + r.maxY()
                                            + " Z " + r.minZ() + "…" + r.maxZ(),
                                    NamedTextColor.WHITE)));
                    Msg.server(player, Msg.info(
                            "Home: " + (r.hasHome() ? "есть (/apriv home " + r.name() + ")" : "нет")
                                    + " | Блоки для игроков: "
                                    + (r.allowMemberInteract() ? "вкл" : "выкл")));
                }
                case "sethome", "дом" -> {
                    if (args.length < 2) {
                        Msg.server(player, Msg.err("Использование: /apriv sethome <имя>"));
                        return true;
                    }
                    String err = regions.setRegionHome(player, args[1]);
                    if (err != null) {
                        Msg.server(player, Msg.err(err));
                    } else {
                        Msg.server(player, Msg.ok("Home региона «" + args[1].toLowerCase(Locale.ROOT) + "» установлен."));
                    }
                }
                case "home", "тп" -> {
                    if (args.length < 2) {
                        Msg.server(player, Msg.err("Использование: /apriv home <имя>"));
                        return true;
                    }
                    String err = regions.teleportRegionHome(player, args[1]);
                    if (err != null) {
                        Msg.server(player, Msg.err(err));
                    }
                }
                case "delhome" -> {
                    if (args.length < 2) {
                        Msg.server(player, Msg.err("Использование: /apriv delhome <имя>"));
                        return true;
                    }
                    String err = regions.clearRegionHome(args[1]);
                    if (err != null) {
                        Msg.server(player, Msg.err(err));
                    } else {
                        Msg.server(player, Msg.ok("Home удалён."));
                    }
                }
                case "interact", "сундуки", "двери" -> {
                    if (args.length < 2) {
                        Msg.server(player, Msg.err("Использование: /apriv interact <имя> [on|off]"));
                        return true;
                    }
                    Boolean on = null;
                    if (args.length >= 3) {
                        String v = args[2].toLowerCase(Locale.ROOT);
                        if (v.equals("on") || v.equals("1") || v.equals("вкл") || v.equals("да")) {
                            on = true;
                        } else if (v.equals("off") || v.equals("0") || v.equals("выкл") || v.equals("нет")) {
                            on = false;
                        } else {
                            Msg.server(player, Msg.err("on или off"));
                            return true;
                        }
                    }
                    String err = regions.setMemberInteract(player, args[1], on);
                    if (err != null) {
                        Msg.server(player, Msg.err(err));
                    }
                }
                case "clear", "сброс" -> regions.clearSelection(player);
                default -> sendUsage(player);
            }
        } catch (SQLException e) {
            Msg.server(player, Msg.err("Ошибка БД."));
        }
        return true;
    }

    private void sendUsage(Player player) {
        Msg.server(player, Msg.info("//wand → ЛКМ/ПКМ → /expand N up|down → /claim <имя>"));
        Msg.server(player, Component.text(
                "/apriv sethome|home|delhome <имя> | interact <имя> [on|off] | list | delete | info | clear",
                NamedTextColor.GRAY));
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player) || !regions.canManage(player)) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(args[0], "wand", "delete", "list", "info", "clear",
                    "sethome", "home", "delhome", "interact", "сундуки");
        }
        if (args.length == 2 && needsRegionName(args[0])) {
            return filter(args[1], regionNames());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("interact")) {
            return filter(args[2], "on", "off");
        }
        return List.of();
    }

    private static boolean needsRegionName(String sub) {
        return switch (sub.toLowerCase(Locale.ROOT)) {
            case "delete", "удалить", "sethome", "дом", "home", "тп", "delhome", "interact", "сундуки", "двери" -> true;
            default -> false;
        };
    }

    private String[] regionNames() {
        try {
            List<String> names = new ArrayList<>();
            for (AdminRegion r : regions.repo().listAll()) {
                names.add(r.name());
            }
            return names.toArray(String[]::new);
        } catch (SQLException e) {
            return new String[0];
        }
    }

    private static List<String> filter(String prefix, String... options) {
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
