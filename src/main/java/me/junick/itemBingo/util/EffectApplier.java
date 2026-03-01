package me.junick.itemBingo.util;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.enums.BingoEffect;
import me.junick.itemBingo.events.LavaMovement;
import me.junick.itemBingo.model.PlayerBingoProgress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
            if (type == PotionEffectType.UNLUCK) {
                switch(eff) {
                    case STEP_HEIGHT -> {
                        var step = p.getAttribute(Attribute.STEP_HEIGHT);
                        if (step != null) {
                            step.setBaseValue(0.5 + 1.0 * lvl);
                        }
                    }
                    case EFFICIENCY -> {
                        var item = p.getInventory().getItemInMainHand();
                        if (!item.isEmpty()) {
                            if (Enchantment.EFFICIENCY.canEnchantItem(item)) {
                                int enchlvl = item.getEnchantmentLevel(Enchantment.EFFICIENCY);
                                if (enchlvl < lvl) {
                                    item.addEnchantment(Enchantment.EFFICIENCY, lvl);
                                }
                            }
                            p.getInventory().setItemInMainHand(item);
                        }

                    }
                    case MENDING -> {
                        var item = p.getInventory().getItemInMainHand();
                        if (!item.isEmpty()) {
                            if (Enchantment.MENDING.canEnchantItem(item)) {
                                int enchlvl = item.getEnchantmentLevel(Enchantment.MENDING);
                                if (enchlvl < lvl) {
                                    item.addEnchantment(Enchantment.MENDING, lvl);
                                }
                            }
                            p.getInventory().setItemInMainHand(item);
                        }
                    }
                    case IMPROVE_LAVA_MOVEMENT -> {
                        if (lvl == 0) {
                            LavaMovement.removePlayer(p);
                        } else {
                            LavaMovement.addPlayer(p);
                        }
                    }
                }
                continue;
            }
            if (type == PotionEffectType.DOLPHINS_GRACE && lvl > 1) {
                var waterMovement = p.getAttribute(Attribute.WATER_MOVEMENT_EFFICIENCY);
                if (waterMovement != null) {
                    waterMovement.setBaseValue(0.0 + 0.1 * (lvl - 1));
                }
            }
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
            case STEP_HEIGHT, EFFICIENCY, MENDING, IMPROVE_LAVA_MOVEMENT -> PotionEffectType.UNLUCK;
            case CONDUIT_POWER -> PotionEffectType.CONDUIT_POWER;
        };
    }
}
