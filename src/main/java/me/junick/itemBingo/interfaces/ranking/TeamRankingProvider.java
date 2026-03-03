package me.junick.itemBingo.interfaces.ranking;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.model.TeamBingoProgress;
import me.junick.itemBingo.records.ranking.RankingEntry;
import me.junick.itemBingo.util.TeamDataManager;
import me.junick.itemBingo.util.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;

public class TeamRankingProvider implements RankingProvider {
    @Override
    public List<RankingEntry> getRankings(int limit) {
        TeamManager tm = ItemBingo.getInstance().getTeamManager();

        Map<Integer, TeamBingoProgress> allProgress = TeamDataManager.getAllData();

        int teamCount = tm.getTeamCount();
        for (int teamId = 0; teamId < teamCount; teamId++) {
            allProgress.put(teamId, TeamDataManager.get(teamId));
        }

        List<Map.Entry<Integer, TeamBingoProgress>> entries =
                new ArrayList<>(allProgress.entrySet());

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

        int realLimit = (limit <= 0)
                ? entries.size()
                : Math.min(limit, entries.size());

        List<RankingEntry> out = new ArrayList<>();

        for (int i = 0; i < realLimit; i++) {
            int teamId = entries.get(i).getKey();
            TeamBingoProgress prog = entries.get(i).getValue();

            int score = prog.getScore();
            long penalty = prog.getTotalSubmitTime();

            String name = buildTeamName(teamId, tm);

            switch(Settings.getPenaltySystem()) {
                case TOTAL_SUBMISSION -> out.add(new RankingEntry(name, prog.getScore(), prog.getTotalSubmitTime()));
                case LAST_SUBMISSION -> out.add(new RankingEntry(name, prog.getScore(), prog.getMaxSubmitTime()));
                case CODEFORCES -> out.add(new RankingEntry(name, (int)prog.getCodeforcesScore(), prog.getScore()));
            }
        }
        return out;
    }

    private String buildTeamName(int teamId, TeamManager tm) {
        List<String> members = tm.getPlayersOnTeam(teamId).stream()
                .map(Bukkit::getOfflinePlayer)
                .map(OfflinePlayer::getName)
                .filter(Objects::nonNull)
                .toList();

        String summary = summarizeMembers(members, 15);
        return "Team " + (teamId + 1) + " (" + summary + ")";
    }

    private String summarizeMembers(List<String> names, int maxChars) {
        if (names == null || names.isEmpty()) return "-";

        StringBuilder sb = new StringBuilder();
        for (String name : names) {
            String add = (sb.isEmpty() ? "" : ", ") + (name.length() > 3 ? name.substring(0, 3) : name);

            if (sb.length() + add.length() > maxChars) {
                if (sb.isEmpty()) {
                    String n = name;
                    int cut = Math.max(1, maxChars - 3);
                    return n.substring(0, Math.min(cut, n.length())) + "...";
                }
                return sb + "...";
            }

            sb.append(add);
        }
        return sb.toString();
    }
}
