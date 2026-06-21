package me.junick.itemBingo.interfaces.access;

import me.junick.itemBingo.enums.BingoRewardType;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public interface BingoProgressAccess {
    boolean isSubmitted(int idx);
    void submit(int idx);
    void submit(int idx, Player submitter);

    UUID getSubmitterId(int idx);
    String getSubmitterName(int idx);
    int getSubmissionCount(UUID submitterId);

    void addCurrencyAll(BingoRewardType type, int amount);
    void addCurrency(Player p, BingoRewardType type, int amount);
    int getCurrencyAll(BingoRewardType type);
    int getCurrency(Player p, BingoRewardType type);

    Set<Integer> getSubmittedSlots();

    void save();

    Iterable<Player> viewers(Player trigger);
}
