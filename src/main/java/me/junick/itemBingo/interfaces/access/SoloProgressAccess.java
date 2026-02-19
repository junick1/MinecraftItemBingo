package me.junick.itemBingo.interfaces.access;

import me.junick.itemBingo.enums.BingoRewardType;
import me.junick.itemBingo.model.PlayerBingoProgress;
import me.junick.itemBingo.util.PlayerDataManager;
import org.bukkit.entity.Player;

import java.util.Set;

public class SoloProgressAccess implements BingoProgressAccess {
    private final Player owner;
    private final PlayerBingoProgress prog;

    public SoloProgressAccess(Player owner) {
        this.owner = owner;
        this.prog = PlayerDataManager.get(owner);
    }

    @Override public boolean isSubmitted(int idx) { return prog.isSubmitted(idx); }
    @Override public void submit(int idx) { prog.submit(idx); }

    @Override public void addCurrency(BingoRewardType type, int amount) { prog.addCurrency(type, amount); }
    @Override public int getCurrency(BingoRewardType type) { return prog.getCurrency(type); }

    @Override public Set<Integer> getSubmittedSlots() { return prog.getSubmittedSlots(); }

    @Override public void save() { PlayerDataManager.save(owner); }

    @Override public Iterable<Player> viewers(Player trigger) { return java.util.List.of(owner); }
}
