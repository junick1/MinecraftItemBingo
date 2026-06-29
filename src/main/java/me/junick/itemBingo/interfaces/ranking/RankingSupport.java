package me.junick.itemBingo.interfaces.ranking;

import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.model.BingoProgress;
import me.junick.itemBingo.records.ranking.RankingEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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

    /**
     * Sorts {@code progressByKey} best-first for the given {@code penalty} and
     * returns the top {@code limit} as display entries ({@code limit <= 0} means
     * "all"). {@code nameFn} resolves each key to its display name. Shared by the
     * solo and team ranking providers so the sort/limit logic lives in one place.
     */
    public static <K, P extends BingoProgress> List<RankingEntry> rank(
            Map<K, P> progressByKey, int limit, Settings.Penalty penalty, Function<K, String> nameFn) {

        List<Map.Entry<K, P>> entries = new ArrayList<>(progressByKey.entrySet());
        entries.sort(Map.Entry.comparingByValue(comparator(penalty)));

        int realLimit = (limit <= 0) ? entries.size() : Math.min(limit, entries.size());

        List<RankingEntry> out = new ArrayList<>(realLimit);
        for (int i = 0; i < realLimit; i++) {
            Map.Entry<K, P> e = entries.get(i);
            out.add(toEntry(nameFn.apply(e.getKey()), e.getValue(), penalty));
        }
        return out;
    }
}
