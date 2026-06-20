package me.junick.itemBingo.interfaces.ranking;

import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.model.BingoProgress;
import me.junick.itemBingo.records.ranking.RankingEntry;

import java.util.Comparator;

/**
 * Shared sorting / entry-building logic for ranking providers.
 * Keeps {@link SoloRankingProvider} and {@link TeamRankingProvider} in sync
 * so a new penalty mode only needs to be handled in one place.
 */
public final class RankingSupport {

    private RankingSupport() {}

    /** Comparator that orders progress best-first for the given penalty system. */
    public static Comparator<BingoProgress> comparator(Settings.Penalty penalty) {
        return switch (penalty) {
            case TOTAL_SUBMISSION -> Comparator
                    .comparingInt(BingoProgress::getScore).reversed()
                    .thenComparingLong(BingoProgress::getTotalSubmitTime);
            case LAST_SUBMISSION -> Comparator
                    .comparingInt(BingoProgress::getScore).reversed()
                    .thenComparingLong(BingoProgress::getMaxSubmitTime);
            case CODEFORCES -> Comparator
                    .comparingLong(BingoProgress::getCodeforcesScore).reversed()
                    .thenComparing(Comparator.comparingInt(BingoProgress::getScore).reversed());
        };
    }

    /** Builds the display entry for a single ranked progress. */
    public static RankingEntry toEntry(String name, BingoProgress prog, Settings.Penalty penalty) {
        return switch (penalty) {
            case TOTAL_SUBMISSION -> new RankingEntry(name, prog.getScore(), prog.getTotalSubmitTime());
            case LAST_SUBMISSION -> new RankingEntry(name, prog.getScore(), prog.getMaxSubmitTime());
            case CODEFORCES -> new RankingEntry(name, (int) prog.getCodeforcesScore(), prog.getScore());
        };
    }
}
