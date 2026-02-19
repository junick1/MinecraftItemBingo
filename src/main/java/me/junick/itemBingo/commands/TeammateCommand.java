package me.junick.itemBingo.commands;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.util.TeamManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class TeammateCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can run this command!");
            return true;
        }

        TeamManager tm = ItemBingo.getInstance().getTeamManager();
        Player p = (Player) sender;

        int teamId = tm.getTeamId(p);
        if (teamId == TeamManager.NO_TEAM) {
            sender.sendMessage("§cYou're not in a team!");
            return true;
        }

        if (teamId < 0 || teamId >= tm.getTeamCount()) {
            sender.sendMessage("§cError! Team # out of range. (1.." + tm.getTeamCount() + ")");
            return true;
        }

        List<Player> online = tm.getOnlinePlayersOnTeam(teamId);
        p.sendMessage("§b[Team " + (teamId + 1) + "] §f(" + online.size() + "명) §7" + joinNames(online));
        return true;
    }

    private String joinNames(List<? extends OfflinePlayer> ps) {
        if (ps == null || ps.isEmpty()) return "§7(없음)";
        return ps.stream().map(this::name).collect(Collectors.joining("§7, §f"));
    }

    private String name(OfflinePlayer p) {
        if (p == null) return "(null)";
        String n = p.getName();
        return (n != null ? n : p.getUniqueId().toString());
    }
}
