package me.junick.itemBingo.util;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import me.junick.itemBingo.records.ranking.RankingEntry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;

public class BingoScoreboard {
    private static final String OBJECTIVE_NAME = "bingo_sidebar";
    private static final int MAX_ROWS = 14;

    /**
     * The sidebar is a single shared scoreboard shown to every player, so it can't
     * be localized per-player; it renders in the server default language.
     */
    private static SupportedLocale loc() {
        return Settings.getDefaultLanguage();
    }

    private static final Scoreboard BOARD = Bukkit.getScoreboardManager().getNewScoreboard();
    private static Objective objective;

    public static void updateAll() {
        if (ItemBingo.currentBingo == null) return;

        initializeObjective();
        clearScores();

        if (Leaderboard.isHidden()) {
            showHiddenPlaceholder();
            return;
        }

        List<RankingEntry> rankings = getSortedScores();
        updatePlayerScores(rankings);
    }

    /** Replaces the rankings with a "revealed after the game" notice while hidden. */
    private static void showHiddenPlaceholder() {
        objective.getScore(" ").setScore(4);
        objective.getScore(Messages.legacy(loc(), "scoreboard.hidden1")).setScore(3);
        objective.getScore(Messages.legacy(loc(), "scoreboard.hidden2")).setScore(2);
        objective.getScore("  ").setScore(1);
        assignToViewers();
    }

    private static void initializeObjective() {
        if (objective != null) return;

        objective = BOARD.registerNewObjective(OBJECTIVE_NAME, "dummy",
                Messages.legacy(loc(), "scoreboard.title"));
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

        assignToViewers();
    }

    /**
     * Points every online player at the shared sidebar, but only if they aren't
     * already viewing it. The sidebar is a single shared {@link Scoreboard}, so
     * score changes propagate to current viewers for free — re-assigning the same
     * board every tick just sends redundant packets. Guarding on the current
     * board means a freshly joined player is picked up within a tick while
     * everyone else is left untouched.
     */
    private static void assignToViewers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getScoreboard() != BOARD) p.setScoreboard(BOARD);
        }
    }

    private static String formatEntry(int rank, String name, int score, long totalSeconds) {
        String color = switch (rank) {
            case 1 -> "§6"; // Gold for 1st place
            case 2 -> "§7"; // Silver for 2nd place
            case 3 -> "§c"; // Bronze for 3rd place
            default -> "§f"; // White for others
        };

        StringBuilder sb = new StringBuilder(48);
        sb.append(color).append(Messages.legacy(loc(), "scoreboard.rank-fmt", "rank", rank)).append(" §f").append(name)
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
