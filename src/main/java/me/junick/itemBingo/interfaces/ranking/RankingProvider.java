package me.junick.itemBingo.interfaces.ranking;

import me.junick.itemBingo.records.ranking.RankingEntry;

import java.util.List;

public interface RankingProvider {
    List<RankingEntry> getRankings(int limit);
}
