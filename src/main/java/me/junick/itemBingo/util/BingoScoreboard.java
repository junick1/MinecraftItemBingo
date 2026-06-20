package me.junick.itemBingo.util;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.records.ranking.RankingEntry;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;

public class BingoScoreboard {
    private static final String OBJECTIVE_NAME = "bingo_sidebar";
    private static final String OBJECTIVE_DISPLAY_NAME = "§e§l빙고 진행 상황";
    private static final int MAX_ROWS = 14;

    private static final Scoreboard BOARD = Bukkit.getScoreboardManager().getNewScoreboard();
    private static Objective objective;

    public static void updateAll() {
        if (ItemBingo.currentBingo == null) return;

        initializeObjective();
        clearScores();

        List<RankingEntry> rankings = getSortedScores();
        updatePlayerScores(rankings);
    }

    private static void initializeObjective() {
        if (objective != null) return;

        objective = BOARD.registerNewObjective(OBJECTIVE_NAME, "dummy", OBJECTIVE_DISPLAY_NAME);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        NumberFormat hideFormat = NumberFormat.blank();
        objective.numberFormat(hideFormat);
    }

    private static void clearScores() {
        for (String entry : BOARD.getEntries()) {
            BOARD.resetScores(entry);
        }
    }

    private static List<RankingEntry> getSortedScores() {
        return RankingProviders.current().getRankings(MAX_ROWS);
    }

    private static void updatePlayerScores(List<RankingEntry> rankings) {
        int scoreRow = MAX_ROWS;
        int rank = 1;

        for (RankingEntry pr : rankings) {
            String entryName = formatEntry(rank, pr.displayName(), pr.score(), pr.penaltySeconds());
            objective.getScore(entryName).setScore(scoreRow);

            rank++;
            scoreRow--;
        }

        int pad = 1;
        for (; scoreRow > 0; scoreRow--) {
            String placeholder = " ".repeat(pad++);
            objective.getScore(placeholder).setScore(scoreRow);
        }

        Bukkit.getOnlinePlayers().forEach(p -> p.setScoreboard(BOARD));
    }

    private static String formatEntry(int rank, String name, int score, long totalSeconds) {
        String color = switch (rank) {
            case 1 -> "§6"; // Gold for 1st place
            case 2 -> "§7"; // Silver for 2nd place
            case 3 -> "§c"; // Bronze for 3rd place
            default -> "§f"; // White for others
        };

        StringBuilder sb = new StringBuilder(48);
        sb.append(color).append(rank).append("위 §f").append(name)
          .append(" - §b").append(score).append(" §7(");

        if (Settings.getPenaltySystem() == Settings.Penalty.CODEFORCES) {
            sb.append(totalSeconds);
        } else {
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;
            if (minutes < 10) sb.append('0');
            sb.append(minutes).append(':');
            if (seconds < 10) sb.append('0');
            sb.append(seconds);
        }
        return sb.append(')').toString();
    }
}
