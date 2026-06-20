package me.junick.itemBingo.commands;

import me.junick.itemBingo.ItemBingo;
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
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        TeamManager tm = ItemBingo.getInstance().getTeamManager();

        int teamId = tm.getTeamId(p);
        if (teamId == TeamManager.NO_TEAM) {
            sender.sendMessage("§c팀에 속해 있지 않습니다!");
            return true;
        }

        if (teamId < 0 || teamId >= tm.getTeamCount()) {
            sender.sendMessage("§c오류! 팀 번호가 범위를 벗어났습니다. (1.." + tm.getTeamCount() + ")");
            return true;
        }

        List<Player> online = tm.getOnlinePlayersOnTeam(teamId);
        p.sendMessage("§b[팀 " + (teamId + 1) + "] §f(" + online.size() + "명) §7" + joinNames(online));
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
