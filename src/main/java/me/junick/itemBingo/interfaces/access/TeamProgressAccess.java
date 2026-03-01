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

    @Override public void addCurrency(BingoRewardType type, int amount) {
        prog.addCurrency(type, amount);
        if (type != BingoRewardType.LINE) return;
        TeamManager tm = ItemBingo.getInstance().getTeamManager();
        Set<UUID> member = tm.getPlayersOnTeam(teamId);
        for (UUID u : member) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                PlayerDataManager.get(p).addCurrency(type, amount);
            }
        }
    }
    @Override public int getCurrency(BingoRewardType type) { return prog.getCurrency(type); }

    @Override public Set<Integer> getSubmittedSlots() { return prog.getSubmittedSlots(); }

    @Override public void save() { TeamDataManager.save(teamId); }

    @Override
    public Iterable<Player> viewers(Player trigger) {
        TeamManager tm = ItemBingo.getInstance().getTeamManager();
        return tm.getOnlinePlayersOnTeam(teamId);
    }

    public int getTeamId() { return teamId; }
}
