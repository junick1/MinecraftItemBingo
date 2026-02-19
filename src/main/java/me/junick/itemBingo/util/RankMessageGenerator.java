package me.junick.itemBingo.util;

import me.junick.itemBingo.interfaces.ranking.RankingProvider;
import me.junick.itemBingo.records.ranking.RankingEntry;
import org.bukkit.command.CommandSender;

import java.util.*;

public class RankMessageGenerator {
    public static void sendRankMessage(CommandSender sender) {
        for (String msg : generateRankMessage()) sender.sendMessage(msg);
    }

    public static List<String> generateRankMessage() {
        RankingProvider provider = RankingProviders.current();
        List<RankingEntry> rankings = provider.getRankings(0);

        List<String> messages = new ArrayList<>();
        messages.add("");
        messages.add("§6===== §e빙고 랭킹 §6=====");

        int rank = 1;
        for (RankingEntry e : rankings) {
            String time = String.format("%02d:%02d", e.penaltySeconds() / 60, e.penaltySeconds() % 60);
            messages.add("§f" + (rank++) + ". §a" + e.displayName()
                    + "§f - §b" + e.score() + "개§f, (§7" + time + "§f)");
        }
        return messages;
    }
}
