package me.junick.itemBingo.model;

import me.junick.itemBingo.enums.BingoEffect;
import me.junick.itemBingo.enums.BingoRewardType;
import me.junick.itemBingo.util.TimerManager;

import java.util.*;

public class PlayerBingoProgress extends BingoProgress {
    protected EnumMap<BingoEffect, Integer> effectLevels = new EnumMap<>(BingoEffect.class);

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
