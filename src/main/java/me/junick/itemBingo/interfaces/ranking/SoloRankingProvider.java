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

        return RankingSupport.rank(allProgress, limit, Settings.getPenaltySystem(), id -> {
            OfflinePlayer op = Bukkit.getOfflinePlayer(id);
            return (op.getName() != null) ? op.getName()
                    : Messages.legacy(Settings.getDefaultLanguage(), "rank.unknown-player");
        });
    }
}
