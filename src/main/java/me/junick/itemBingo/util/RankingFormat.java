package me.junick.itemBingo.util;

import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;

/**
 * Shared formatting for ranking penalties so the scoreboard, {@code /rank}, the
 * end-of-game title and the results summary all render a record the same way.
 */
public final class RankingFormat {
    private RankingFormat() {}

    /** Penalty as raw points in Codeforces mode, otherwise {@code mm:ss}. */
    public static String penalty(long totalSeconds, SupportedLocale loc) {
        if (Settings.getPenaltySystem() == Settings.Penalty.CODEFORCES) {
            return Messages.legacy(loc, "rank.points", "n", totalSeconds);
        }
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /** Penalty in the server default language (for shared/global displays). */
    public static String penalty(long totalSeconds) {
        return penalty(totalSeconds, Settings.getDefaultLanguage());
    }
}
