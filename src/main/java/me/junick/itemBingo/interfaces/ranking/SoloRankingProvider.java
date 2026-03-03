package me.junick.itemBingo.interfaces.ranking;

import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.model.PlayerBingoProgress;
import me.junick.itemBingo.records.ranking.RankingEntry;
import me.junick.itemBingo.util.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class SoloRankingProvider implements RankingProvider {
    @Override
    public List<RankingEntry> getRankings(int limit) {
        Map<UUID, PlayerBingoProgress> allProgress = PlayerDataManager.getAllData();

        for (Player p : Bukkit.getOnlinePlayers()) {
            allProgress.put(p.getUniqueId(), PlayerDataManager.get(p));
        }

        List<Map.Entry<UUID, PlayerBingoProgress>> entries = new ArrayList<>(allProgress.entrySet());
        if (Settings.getPenaltySystem() == Settings.Penalty.TOTAL_SUBMISSION) {
            entries.sort((a, b) -> {
                int cmp = Integer.compare(b.getValue().getScore(), a.getValue().getScore());
                if (cmp != 0) return cmp;
                return Long.compare(a.getValue().getTotalSubmitTime(), b.getValue().getTotalSubmitTime());
            });
        }
        if (Settings.getPenaltySystem() == Settings.Penalty.LAST_SUBMISSION) {
            entries.sort((a, b) -> {
                int cmp = Integer.compare(b.getValue().getScore(), a.getValue().getScore());
                if (cmp != 0) return cmp;
                return Long.compare(a.getValue().getMaxSubmitTime(), b.getValue().getMaxSubmitTime());
            });
        }
        if (Settings.getPenaltySystem() == Settings.Penalty.CODEFORCES) {
            entries.sort((a, b) -> {
                int cmp = Long.compare(b.getValue().getCodeforcesScore(), a.getValue().getCodeforcesScore());
                if (cmp != 0) return cmp;
                return Long.compare(b.getValue().getScore(), a.getValue().getScore());
            });
        }
        int realLimit = (limit <= 0) ? entries.size() : Math.min(limit, entries.size());

        List<RankingEntry> out = new ArrayList<>();
        for (int i = 0; i < realLimit; i++) {
            UUID id = entries.get(i).getKey();
            PlayerBingoProgress prog = entries.get(i).getValue();

            OfflinePlayer op = Bukkit.getOfflinePlayer(id);
            String name = (op.getName() != null) ? op.getName() : "알 수 없는 플레이어";

            switch(Settings.getPenaltySystem()) {
                case TOTAL_SUBMISSION -> out.add(new RankingEntry(name, prog.getScore(), prog.getTotalSubmitTime()));
                case LAST_SUBMISSION -> out.add(new RankingEntry(name, prog.getScore(), prog.getMaxSubmitTime()));
                case CODEFORCES -> out.add(new RankingEntry(name, (int)prog.getCodeforcesScore(), prog.getScore()));
            }

        }
        return out;
    }
}
