package me.junick.itemBingo.interfaces.access;

import me.junick.itemBingo.enums.BingoRewardType;
import org.bukkit.entity.Player;

import java.util.Set;

public interface BingoProgressAccess {
    boolean isSubmitted(int idx);
    void submit(int idx);

    void addCurrency(BingoRewardType type, int amount);
    int getCurrency(BingoRewardType type);

    Set<Integer> getSubmittedSlots();

    void save();

    Iterable<Player> viewers(Player trigger);
}
