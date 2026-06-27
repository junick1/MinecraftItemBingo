package me.junick.itemBingo.util;

import me.junick.itemBingo.config.Settings;

/**
 * Shared formatting for ranking penalties so the scoreboard, {@code /rank}, the
 * end-of-game title and the results summary all render a record the same way.
 */
public final class RankingFormat {
    private RankingFormat() {}

    /** Penalty as raw points in 점수제 (Codeforces) mode, otherwise {@code mm:ss}. */
    public static String penalty(long totalSeconds) {
        if (Settings.getPenaltySystem() == Settings.Penalty.CODEFORCES) {
            return totalSeconds + "점";
        }
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
