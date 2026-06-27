package me.junick.itemBingo.commands;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import me.junick.itemBingo.util.TeamManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class TeammateCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Messages.get(sender, "command.players-only"));
            return true;
        }

        SupportedLocale loc = Messages.localeOf(p);
        TeamManager tm = ItemBingo.getInstance().getTeamManager();

        int teamId = tm.getTeamId(p);
        if (teamId == TeamManager.NO_TEAM) {
            p.sendMessage(Messages.get(p, "command.teammate.no-team"));
            return true;
        }

        if (teamId < 0 || teamId >= tm.getTeamCount()) {
            p.sendMessage(Messages.get(p, "command.teammate.out-of-range", "count", tm.getTeamCount()));
            return true;
        }

        List<Player> online = tm.getOnlinePlayersOnTeam(teamId);
        String names = online.isEmpty() ? Messages.legacy(loc, "command.teammate.none") : joinNames(online);
        p.sendMessage(Messages.get(p, "command.teammate.list",
                "team", teamId + 1, "count", online.size(), "names", names));
        return true;
    }

    private String joinNames(List<? extends OfflinePlayer> ps) {
        return ps.stream().map(this::name).collect(Collectors.joining("§7, §f"));
    }

    private String name(OfflinePlayer p) {
        if (p == null) return "(null)";
        String n = p.getName();
        return (n != null ? n : p.getUniqueId().toString());
    }
}
