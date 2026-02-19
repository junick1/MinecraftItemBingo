package me.junick.itemBingo.model;

import me.junick.itemBingo.enums.BingoRewardType;
import me.junick.itemBingo.util.TimerManager;

import java.util.*;

public class TeamBingoProgress {
    private final int teamId;

    private final Set<Integer> submittedSlots = new HashSet<>();
    private final Map<Integer, Long> submissionTimes = new HashMap<>();

    private final EnumMap<BingoRewardType, Integer> currency = new EnumMap<>(BingoRewardType.class);

    public TeamBingoProgress(int teamId) {
        this.teamId = teamId;

        for (BingoRewardType t : BingoRewardType.values()) currency.put(t, 0);
    }

    /**
     * Basic info
     */

    public int getTeamId() {
        return teamId;
    }

    public int getScore() {
        return submittedSlots.size();
    }

    /**
     * Submission
     */

    public void submit(int idx, boolean penalty) {
        submittedSlots.add(idx);

        if (TimerManager.isRunning() && penalty) {
            submissionTimes.put(idx, (long) TimerManager.getElapsedSeconds());
        }
    }

    public void submit(int idx) {
        submit(idx, true);
    }

    public boolean isSubmitted(int idx) {
        return submittedSlots.contains(idx);
    }

    public Set<Integer> getSubmittedSlots() {
        return Collections.unmodifiableSet(submittedSlots);
    }

    public Map<Integer, Long> getSubmissionTimes() {
        return submissionTimes;
    }

    public long getTotalSubmitTime() {
        long total = 0;
        for (long time : submissionTimes.values()) {
            total += time;
        }
        return total;
    }

    /**
     * Currency
     */
    public void addCurrency(BingoRewardType type, int amount) {
        int current = currency.getOrDefault(type, 0);
        currency.put(type, Math.max(0, current + amount));
    }

    public int getCurrency(BingoRewardType type) {
        return currency.getOrDefault(type, 0);
    }

    public void setCurrency(BingoRewardType type, int amount) {
        currency.put(type, Math.max(0, amount));
    }

    public EnumMap<BingoRewardType, Integer> getAllCurrency() {
        return currency;
    }
}
