package me.junick.itemBingo.util;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.interfaces.access.BingoProgressAccess;
import me.junick.itemBingo.interfaces.access.SoloProgressAccess;
import me.junick.itemBingo.interfaces.access.TeamProgressAccess;
import org.bukkit.entity.Player;

public class ProgressFactory {
    public static BingoProgressAccess of(Player p) {
        TeamManager tm = ItemBingo.getInstance().getTeamManager();
        int teamId = tm.getTeamId(p);

        if (teamId == TeamManager.NO_TEAM) {
            return new SoloProgressAccess(p);
        }
        return new TeamProgressAccess(teamId);
    }
}
