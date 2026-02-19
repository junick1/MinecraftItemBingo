package me.junick.itemBingo.interfaces.ranking;

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
        entries.sort((a, b) -> {
            int cmp = Integer.compare(b.getValue().getScore(), a.getValue().getScore());
            if (cmp != 0) return cmp;
            return Long.compare(a.getValue().getTotalSubmitTime(), b.getValue().getTotalSubmitTime());
        });

        int realLimit = (limit <= 0) ? entries.size() : Math.min(limit, entries.size());

        List<RankingEntry> out = new ArrayList<>();
        for (int i = 0; i < realLimit; i++) {
            UUID id = entries.get(i).getKey();
            PlayerBingoProgress prog = entries.get(i).getValue();

            OfflinePlayer op = Bukkit.getOfflinePlayer(id);
            String name = (op.getName() != null) ? op.getName() : "알 수 없는 플레이어";

            out.add(new RankingEntry(name, prog.getScore(), prog.getTotalSubmitTime()));
        }
        return out;
    }
}
