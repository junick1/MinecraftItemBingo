package me.junick.itemBingo.interfaces.access;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.enums.BingoRewardType;
import me.junick.itemBingo.model.TeamBingoProgress;
import me.junick.itemBingo.util.PlayerDataManager;
import me.junick.itemBingo.util.TeamDataManager;
import me.junick.itemBingo.util.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class TeamProgressAccess implements BingoProgressAccess {
    private final int teamId;
    private final TeamBingoProgress prog;

    public TeamProgressAccess(int teamId) {
        this.teamId = teamId;
        this.prog = TeamDataManager.get(teamId);
    }

    @Override public boolean isSubmitted(int idx) { return prog.isSubmitted(idx); }
    @Override public void submit(int idx) { prog.submit(idx); }

    @Override public void addCurrencyAll(BingoRewardType type, int amount) {
        if (type.isPersonal()) {
            TeamManager tm = ItemBingo.getInstance().getTeamManager();
            for (UUID u : tm.getPlayersOnTeam(teamId)) {
                var data = PlayerDataManager.get(u);
                if (data != null) data.addCurrency(type, amount);
            }
        } else {
            prog.addCurrency(type, amount);
        }
    }

    @Override public void addCurrency(Player p, BingoRewardType type, int amount) {
        var data = PlayerDataManager.get(p);
        data.addCurrency(type, amount);
    }

    @Override public int getCurrencyAll(BingoRewardType type) { return prog.getCurrency(type); }

    @Override public int getCurrency(Player p, BingoRewardType type) {
        if (type.isPersonal()) {
            var data = PlayerDataManager.get(p);
            return data.getCurrency(type);
        } else {
            return prog.getCurrency(type);
        }
    }

    @Override public Set<Integer> getSubmittedSlots() { return prog.getSubmittedSlots(); }

    @Override public void save() { TeamDataManager.save(teamId); }

    @Override
    public Iterable<Player> viewers(Player trigger) {
        TeamManager tm = ItemBingo.getInstance().getTeamManager();
        return tm.getOnlinePlayersOnTeam(teamId);
    }

    public int getTeamId() { return teamId; }
}
