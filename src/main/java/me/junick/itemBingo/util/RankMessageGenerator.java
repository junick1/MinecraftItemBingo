package me.junick.itemBingo.util;

import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import me.junick.itemBingo.interfaces.ranking.RankingProvider;
import me.junick.itemBingo.records.ranking.RankingEntry;
import org.bukkit.command.CommandSender;

import java.util.*;

public class RankMessageGenerator {
    public static void sendRankMessage(CommandSender sender) {
        if (Leaderboard.isHidden()) {
            sender.sendMessage(Messages.get(sender, "rank.hidden"));
            return;
        }
        for (String msg : generateRankMessage(Messages.localeOf(sender))) sender.sendMessage(msg);
    }

    public static List<String> generateRankMessage(SupportedLocale loc) {
        RankingProvider provider = RankingProviders.current();
        List<RankingEntry> rankings = provider.getRankings(0);

        List<String> messages = new ArrayList<>();
        messages.add("");
        messages.add(Messages.legacy(loc, "rank.header"));

        int rank = 1;
        for (RankingEntry e : rankings) {
            String time = String.format("%02d:%02d", e.penaltySeconds() / 60, e.penaltySeconds() % 60);
            messages.add(Messages.legacy(loc, "rank.entry",
                    "rank", rank++, "name", e.displayName(), "score", e.score(), "time", time));
        }
        return messages;
    }
}
