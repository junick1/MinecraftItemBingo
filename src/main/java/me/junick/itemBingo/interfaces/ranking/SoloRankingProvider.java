package me.junick.itemBingo.interfaces.ranking;

import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.i18n.Messages;
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

        Settings.Penalty penalty = Settings.getPenaltySystem();

        List<Map.Entry<UUID, PlayerBingoProgress>> entries = new ArrayList<>(allProgress.entrySet());
        entries.sort(Map.Entry.comparingByValue(RankingSupport.comparator(penalty)));

        int realLimit = (limit <= 0) ? entries.size() : Math.min(limit, entries.size());

        List<RankingEntry> out = new ArrayList<>();
        for (int i = 0; i < realLimit; i++) {
            UUID id = entries.get(i).getKey();
            PlayerBingoProgress prog = entries.get(i).getValue();

            OfflinePlayer op = Bukkit.getOfflinePlayer(id);
            String name = (op.getName() != null) ? op.getName()
                    : Messages.legacy(Settings.getDefaultLanguage(), "rank.unknown-player");

            out.add(RankingSupport.toEntry(name, prog, penalty));
        }
        return out;
    }
}
