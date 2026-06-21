package me.junick.itemBingo.util;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.interfaces.ranking.RankingProvider;
import me.junick.itemBingo.interfaces.ranking.SoloRankingProvider;
import me.junick.itemBingo.interfaces.ranking.TeamRankingProvider;

public class RankingProviders {
    private static final SoloRankingProvider SOLO = new SoloRankingProvider();
    private static final TeamRankingProvider TEAM = new TeamRankingProvider();

    public static boolean isTeamMode() {
        // The admin team-mode toggle is the single source of truth: with it off,
        // assignments are ignored and ranking/scoreboard are purely individual.
        if (!Settings.isTeamEnabled()) return false;

        TeamManager tm = ItemBingo.getInstance().getTeamManager();
        for (int i = 0; i < tm.getTeamCount(); i++) {
            if (!tm.getPlayersOnTeam(i).isEmpty()) return true;
        }
        return false;
    }

    public static RankingProvider current() {
        return isTeamMode() ? TEAM : SOLO;
    }
}
