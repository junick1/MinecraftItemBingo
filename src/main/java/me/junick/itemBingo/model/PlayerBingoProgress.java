package me.junick.itemBingo.model;

import me.junick.itemBingo.enums.BingoEffect;
import me.junick.itemBingo.enums.BingoRewardType;
import me.junick.itemBingo.util.TimerManager;

import java.util.*;

public class PlayerBingoProgress {
    private final Set<Integer> submittedSlots = new HashSet<>();
    private final Map<Integer, Long> submissionTimes = new HashMap<>();

    private final EnumMap<BingoEffect, Integer> effectLevels = new EnumMap<>(BingoEffect.class);

    private int diamondCurrency = 0;
    private int lineCurrency = 0;
    private int slotCurrency = 0;

    public void submit(int index, boolean penalty) {
        submittedSlots.add(index);
        if (TimerManager.isRunning() && penalty) {
            submissionTimes.put(index, (long) TimerManager.getElapsedSeconds());
        }
    }

    public void submit(int index) { submit(index, true); }

    public int getScore() { return submittedSlots.size(); }

    // ==== 기본 진행도 / 패널티 ====
    public boolean isSubmitted(int index) { return submittedSlots.contains(index); }
    public Set<Integer> getSubmittedSlots() { return submittedSlots; }

    public Map<Integer, Long> getSubmissionTimes() { return submissionTimes; }
    public long getTotalSubmitTime() {
        long total = 0;
        for (long time : submissionTimes.values()) {
            total += time;
        }
        return total;
    }

    // ==== 돈 시스템 ====
    public void addCurrency(BingoRewardType type, int amount) {
        if (type == BingoRewardType.DIAMOND) {
            diamondCurrency = Math.max(0, diamondCurrency + amount);
        } else if (type == BingoRewardType.LINE) {
            lineCurrency = Math.max(0, lineCurrency + amount);
        } else if (type == BingoRewardType.SLOT) {
            slotCurrency = Math.max(0, slotCurrency + amount);
        }
    }

    public int getCurrency(BingoRewardType type) {
        if (type == BingoRewardType.DIAMOND) {
            return diamondCurrency;
        } else if (type == BingoRewardType.LINE) {
            return lineCurrency;
        } else if (type == BingoRewardType.SLOT) {
            return slotCurrency;
        }
        return 0;
    }

    public void setCurrency(BingoRewardType type, int amount) {
        if (type == BingoRewardType.DIAMOND) {
            diamondCurrency = Math.max(0, amount);
        } else if (type == BingoRewardType.LINE) {
            lineCurrency = Math.max(0, amount);
        } else if (type == BingoRewardType.SLOT) {
            slotCurrency = Math.max(0, amount);
        }
    }

    // ==== 효과 시스템 ====
    public int getEffectLevel(BingoEffect effect) { return effectLevels.getOrDefault(effect, 0); }

    public boolean upgradeEffect(BingoEffect effect) {
        int cur = getEffectLevel(effect);
        if (getCurrency(BingoRewardType.SLOT) < 2) return false;
        if (cur >= effect.getMaxLevel()) return false;

        effectLevels.put(effect, cur + 1);

        addCurrency(BingoRewardType.SLOT, -2);
        return true;
    }

    public EnumMap<BingoEffect, Integer> getEffectLevels() { return effectLevels; }
    public void setEffectLevels(Map<BingoEffect, Integer> map) {
        effectLevels.clear();
        if (map != null) {
            for (Map.Entry<BingoEffect, Integer> e : map.entrySet()) {
                int clamped = Math.max(0, Math.min(e.getKey().getMaxLevel(), e.getValue()));
                if (clamped > 0) effectLevels.put(e.getKey(), clamped);
            }
        }
    }
}
