package me.junick.itemBingo.model;

import me.junick.itemBingo.enums.BingoRewardType;
import me.junick.itemBingo.util.TimerManager;

import java.util.*;

public class BingoProgress {
    protected Set<Integer> submittedSlots = new HashSet<>();
    protected Map<Integer, Long> submissionTimes = new HashMap<>();

    // Who submitted each slot (used to render a player head on the team board).
    protected Map<Integer, UUID> submitterIds = new HashMap<>();
    protected Map<Integer, String> submitterNames = new HashMap<>();

    protected EnumMap<BingoRewardType, Integer> currency = new EnumMap<>(BingoRewardType.class);

    public BingoProgress() {
        for (BingoRewardType t : BingoRewardType.values()) currency.put(t, 0);
    }

    public int getTeamId() { return -1; }

    public int getScore() {
        return submittedSlots.size();
    }

    public void submit(int idx, boolean penalty) {
        submittedSlots.add(idx);
        if (TimerManager.isRunning() && penalty) {
            submissionTimes.put(idx, (long)TimerManager.getElapsedSeconds());
        } else {
            submissionTimes.put(idx, (long)TimerManager.getLastMaxSeconds());
        }
    }

    public void submit(int idx) {
        submit(idx, true);
    }

    public void submit(int idx, boolean penalty, UUID submitterId, String submitterName) {
        submit(idx, penalty);
        if (submitterId != null) {
            submitterIds.put(idx, submitterId);
            if (submitterName != null) submitterNames.put(idx, submitterName);
        }
    }

    public UUID getSubmitterId(int idx) { return submitterIds.get(idx); }
    public String getSubmitterName(int idx) { return submitterNames.get(idx); }

    /** Number of slots submitted by the given player (their contribution). */
    public int getSubmissionCountBy(UUID submitterId) {
        if (submitterId == null) return 0;
        int count = 0;
        for (UUID id : submitterIds.values()) {
            if (submitterId.equals(id)) count++;
        }
        return count;
    }
    public Map<Integer, UUID> getSubmitterIds() { return submitterIds; }
    public Map<Integer, String> getSubmitterNames() { return submitterNames; }

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

    public long getMaxSubmitTime() {
        long total = 0;
        for (long time : submissionTimes.values()) {
            total = Math.max(total, time);
        }
        return total;
    }

    public long getCodeforcesScore() {
        int POINT = 1000;
        long total = 0;
        long maxTime = TimerManager.getLastMaxSeconds();
        if (maxTime == 0) maxTime = 1;
        for (long time : submissionTimes.values()) {
            total += POINT - time * POINT * 60 / maxTime / 125;
        }
        return total;
    }

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
