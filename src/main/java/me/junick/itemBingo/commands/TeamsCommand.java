package me.junick.itemBingo.commands;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.util.GuiSync;
import me.junick.itemBingo.util.TeamManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TeamsCommand implements CommandExecutor, TabCompleter {
    private TeamManager tm() {
        return ItemBingo.getInstance().getTeamManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 0) {
            help(sender, label);
            return true;
        }

        // Set whenever a subcommand actually changes team membership; drives the
        // GUI refresh below since a player's board/shop dataset depends on it.
        boolean mutated = false;

        switch (args[0].toLowerCase()) {
            case "help" -> help(sender, label);

            case "info" -> info(sender);

            case "count" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /" + label + " count <teamNumber>");
                    return true;
                }

                int n = parseInt(args[1], -1);
                if (n < 1) {
                    sender.sendMessage("§cTeam number must be greater or equal to 1.");
                    return true;
                }
                tm().setTeamCount(n);
                mutated = true;
                Bukkit.broadcast(Component.text("§e[Team] §fTeam # set to §a" + n + "§f."));
            }

            case "clear" -> {
                tm().clearAll();
                mutated = true;
                Bukkit.broadcast(Component.text("§e[Team] §fAll team assignments has been reset."));
            }

            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /" + label + " set <player> <team#(1.." + tm().getTeamCount() + ")>");
                    return true;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (target == null || target.getUniqueId() == null) {
                    sender.sendMessage("§cCannot find the player.");
                    return true;
                }

                int teamOneBased = parseInt(args[2], -1);
                int teamId = teamOneBased - 1;
                if (teamId < 0 || teamId >= tm().getTeamCount()) {
                    sender.sendMessage("§cTeam # out of range. (1.." + tm().getTeamCount() + ")");
                    return true;
                }

                tm().assign(target.getUniqueId(), teamId);
                mutated = true;
                for (Player pl : Bukkit.getOnlinePlayers()) {
                    pl.sendMessage(Messages.get(pl, "command.teams.assigned", "player", name(target), "team", teamOneBased));
                }
            }

            case "remove", "unset" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /" + label + " remove <player>");
                    return true;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (target == null || target.getUniqueId() == null) {
                    sender.sendMessage("§cCannot find the player.");
                    return true;
                }

                tm().unassign(target.getUniqueId());
                mutated = true;
                for (Player pl : Bukkit.getOnlinePlayers()) {
                    pl.sendMessage(Messages.get(pl, "command.teams.unassigned", "player", name(target)));
                }
            }

            case "random" -> {
                // /teams random
                // /teams random keep
                // /teams random reset
                boolean keep = false;
                boolean reset = false;

                for (int i = 1; i < args.length; i++) {
                    String opt = args[i].toLowerCase();
                    if (opt.equals("keep")) keep = true;
                    if (opt.equals("reset")) reset = true;
                }

                if (reset) tm().clearAll();

                Collection<? extends OfflinePlayer> all = Bukkit.getOnlinePlayers();
                tm().assignRandomEven(all, keep);
                mutated = true;

                for (Player pl : Bukkit.getOnlinePlayers()) {
                    pl.sendMessage(Messages.get(pl, "command.teams.random-done", "keep", keep, "reset", reset));
                }
                info(sender);
            }

            case "fill" -> {
                // /teams fill
                Collection<? extends OfflinePlayer> all = Bukkit.getOnlinePlayers();
                tm().assignRandomEven(all, true);
                mutated = true;
                for (Player pl : Bukkit.getOnlinePlayers()) {
                    pl.sendMessage(Messages.get(pl, "command.teams.fill-done"));
                }
                info(sender);
            }

            case "list" -> {
                // /teams list [team(1..N)]
                if (args.length == 1) {
                    listAll(sender);
                } else {
                    int teamOneBased = parseInt(args[1], -1);
                    int teamId = teamOneBased - 1;
                    if (teamId < 0 || teamId >= tm().getTeamCount()) {
                        sender.sendMessage("§cTeam # out of range. (1.." + tm().getTeamCount() + ")");
                        return true;
                    }
                    listTeam(sender, teamId);
                }
            }

            default -> {
                sender.sendMessage("§cUnknown argument.");
                help(sender, label);
            }
        }

        // Membership changed → refresh every open board/shop so each player sees
        // their new team's progress and currency instead of the stale dataset.
        if (mutated) {
            GuiSync.refreshAllGameViews();
        }
        return true;
    }

    private void help(CommandSender s, String label) {
        s.sendMessage("§b/teams help");
        s.sendMessage("§b/teams info §7- Team Status");
        s.sendMessage("§b/teams count <n> §7- Set team count");
        s.sendMessage("§b/teams clear §7- Reset team");
        s.sendMessage("§b/teams set <player> <team(1..N)> §7- Manual assignment");
        s.sendMessage("§b/teams remove <player> §7- Manual detachment");
        s.sendMessage("§b/teams random [keep] [reset] §7- Random assignment");
        s.sendMessage("§b/teams fill §7- Assign randomly (only not assigned)");
        s.sendMessage("§b/teams list [team] §7- Teammate list");
    }

    private void info(CommandSender s) {
        Map<Integer, Integer> sizes = tm().getTeamSizes();
        s.sendMessage(Messages.get(s, "command.teams.info-count", "count", tm().getTeamCount()));
        for (int i = 0; i < tm().getTeamCount(); i++) {
            s.sendMessage(Messages.get(s, "command.teams.info-team", "team", i + 1, "size", sizes.getOrDefault(i, 0)));
        }

        long unassigned = Bukkit.getOnlinePlayers().stream().filter(p -> tm().getTeamId(p) == TeamManager.NO_TEAM).count();
        if (unassigned > 0) {
            s.sendMessage(Messages.get(s, "command.teams.info-unassigned", "count", unassigned));
        }
    }

    private void listAll(CommandSender s) {
        for (int i = 0; i < tm().getTeamCount(); i++) {
            listTeam(s, i);
        }
    }

    private void listTeam(CommandSender s, int teamId) {
        List<Player> online = tm().getOnlinePlayersOnTeam(teamId);
        String names = online.isEmpty()
                ? Messages.legacy(Messages.localeOf(s), "command.teammate.none")
                : joinNames(online);
        s.sendMessage(Messages.get(s, "command.teams.list", "team", teamId + 1, "count", online.size(), "names", names));
    }

    private String joinNames(List<? extends OfflinePlayer> ps) {
        return ps.stream().map(this::name).collect(Collectors.joining("§7, §f"));
    }

    private String name(OfflinePlayer p) {
        if (p == null) return "(null)";
        String n = p.getName();
        return (n != null ? n : p.getUniqueId().toString());
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, @NotNull Command cmd, @NotNull String alias, String[] args) {
        if (!(sender.isOp() || sender.hasPermission("itembingo.teams"))) return List.of();

        if (args.length == 1) {
            return startsWith(List.of("help","info","count","clear","set","remove","unset","random","fill","list"), args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("set") || sub.equals("remove") || sub.equals("unset")) {
                return startsWith(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
            }
            if (sub.equals("count")) {
                return List.of("2","3","4","5");
            }
            if (sub.equals("random")) {
                return startsWith(List.of("keep","reset"), args[1]);
            }
            if (sub.equals("list")) {
                List<String> teams = new ArrayList<>();
                for (int i = 1; i <= tm().getTeamCount(); i++) teams.add(String.valueOf(i));
                return startsWith(teams, args[1]);
            }
        }
        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("set")) {
                List<String> teams = new ArrayList<>();
                for (int i = 1; i <= tm().getTeamCount(); i++) teams.add(String.valueOf(i));
                return startsWith(teams, args[2]);
            }
            if (sub.equals("random")) {
                return startsWith(List.of("keep","reset"), args[2]);
            }
        }
        return List.of();
    }

    private List<String> startsWith(List<String> list, String prefix) {
        if (prefix == null) prefix = "";
        String p = prefix.toLowerCase();
        return list.stream().filter(x -> x.toLowerCase().startsWith(p)).limit(50).toList();
    }
}
