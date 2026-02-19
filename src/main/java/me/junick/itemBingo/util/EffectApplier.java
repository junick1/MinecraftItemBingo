package me.junick.itemBingo.util;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.enums.BingoEffect;
import me.junick.itemBingo.model.PlayerBingoProgress;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class EffectApplier extends BukkitRunnable {
    public static void start() {
        new EffectApplier().runTaskTimer(ItemBingo.getInstance(), 0L, 10L);
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            applyFor(p);
        }
    }

    public static void applyFor(Player p) {
        PlayerBingoProgress prog = PlayerDataManager.get(p);

        p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false, false));

        for (BingoEffect eff : BingoEffect.values()) {
            int lvl = eff.getTotalLevel(prog.getEffectLevel(eff));
            PotionEffectType type = map(eff);
            if (type == null) continue;

            int targetAmp = Math.max(0, lvl - 1);
            PotionEffect cur = p.getPotionEffect(type);

            if (lvl <= 0) {
                continue;
            }

            if (cur != null) {
                int curAmp = cur.getAmplifier();

                if (curAmp > targetAmp) continue;
            }

            p.addPotionEffect(new PotionEffect(type, 60, Math.max(0, lvl-1), true, false, true));
        }
    }

    private static PotionEffectType map(BingoEffect eff) {
        return switch (eff) {
            case SPEED -> PotionEffectType.SPEED;
            case DOLPHINS_GRACE -> PotionEffectType.DOLPHINS_GRACE;
            case HASTE -> PotionEffectType.HASTE;
            case STRENGTH -> PotionEffectType.STRENGTH;
            case RESISTANCE -> PotionEffectType.RESISTANCE;
            case HEALTH_BOOST -> PotionEffectType.HEALTH_BOOST;
            case FIRE_RESISTANCE -> PotionEffectType.FIRE_RESISTANCE;
        };
    }
}
