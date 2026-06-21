package me.junick.itemBingo.interfaces.access;

import me.junick.itemBingo.enums.BingoRewardType;
import me.junick.itemBingo.model.PlayerBingoProgress;
import me.junick.itemBingo.util.PlayerDataManager;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class SoloProgressAccess implements BingoProgressAccess {
    private final Player owner;
    private final PlayerBingoProgress prog;

    public SoloProgressAccess(Player owner) {
        this.owner = owner;
        this.prog = PlayerDataManager.get(owner);
    }

    @Override public boolean isSubmitted(int idx) { return prog.isSubmitted(idx); }
    @Override public void submit(int idx) { prog.submit(idx); }
    // Solo boards keep the barrier placeholder, so no submitter is recorded.
    @Override public void submit(int idx, Player submitter) { prog.submit(idx); }

    @Override public UUID getSubmitterId(int idx) { return null; }
    @Override public String getSubmitterName(int idx) { return null; }
    @Override public int getSubmissionCount(UUID submitterId) { return 0; }

    @Override public void addCurrencyAll(BingoRewardType type, int amount) { prog.addCurrency(type, amount); }
    @Override public void addCurrency(Player p, BingoRewardType type, int amount) { prog.addCurrency(type, amount); }
    @Override public int getCurrencyAll(BingoRewardType type) { return prog.getCurrency(type); }
    @Override public int getCurrency(Player p, BingoRewardType type) { return prog.getCurrency(type); }

    @Override public Set<Integer> getSubmittedSlots() { return prog.getSubmittedSlots(); }

    @Override public void save() { PlayerDataManager.save(owner); }

    @Override public Iterable<Player> viewers(Player trigger) { return java.util.List.of(owner); }
}
