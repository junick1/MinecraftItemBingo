package me.junick.itemBingo.events.gui;

import me.junick.itemBingo.enums.BingoEffect;
import me.junick.itemBingo.gui.BingoGuiHolder;
import me.junick.itemBingo.gui.EffectShopGUI;
import me.junick.itemBingo.gui.ShopGUI;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import me.junick.itemBingo.model.PlayerBingoProgress;
import me.junick.itemBingo.util.EffectApplier;
import me.junick.itemBingo.util.PlayerDataManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class EffectShopClickEvent implements Listener {
    @EventHandler
    public void onEffectShopClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!BingoGuiHolder.is(e.getView().getTopInventory(), BingoGuiHolder.Gui.EFFECT_SHOP)) return;

        e.setCancelled(true);

        if (e.getRawSlot() == EffectShopGUI.SLOT_BACK) {
            ShopGUI.open(p);
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String tag = EffectShopGUI.getTaggedEffect(clicked);
        if (tag == null) return;

        BingoEffect eff;
        try {
            eff = BingoEffect.valueOf(tag);
        } catch (IllegalArgumentException ex) { return; }

        SupportedLocale loc = Messages.localeOf(p);
        PlayerBingoProgress prog = PlayerDataManager.get(p);
        if (prog.upgradeEffect(eff)) {
            PlayerDataManager.save(p);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
            p.sendMessage(Messages.get(p, "gui.effect-shop.upgraded",
                    "effect", eff.displayName(loc), "level", prog.getEffectLevel(eff)));
        } else {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            p.sendMessage(Messages.get(p, "gui.effect-shop.upgrade-failed"));
        }

        EffectShopGUI.open(p);
        EffectApplier.applyFor(p);
    }
}
