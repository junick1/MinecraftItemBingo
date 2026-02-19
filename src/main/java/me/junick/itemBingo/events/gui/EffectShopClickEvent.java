package me.junick.itemBingo.events.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.enums.BingoEffect;
import me.junick.itemBingo.gui.EffectShopGUI;
import me.junick.itemBingo.gui.ShopGUI;
import me.junick.itemBingo.model.PlayerBingoProgress;
import me.junick.itemBingo.util.EffectApplier;
import me.junick.itemBingo.util.PlayerDataManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

        String title = LegacyComponentSerializer.legacySection().serialize(e.getView().title());
        if (!title.equals(EffectShopGUI.TITLE)) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (clicked.getItemMeta().getDisplayName().contains("돌아가기")) {
            ShopGUI.open(p);
            return;
        }

        String tag = EffectShopGUI.getTaggedEffect(clicked);
        if (tag == null) return;

        BingoEffect eff;
        try {
            eff = BingoEffect.valueOf(tag);
        } catch (IllegalArgumentException ex) { return; }

        PlayerBingoProgress prog = PlayerDataManager.get(p);
        if (prog.upgradeEffect(eff)) {
            PlayerDataManager.save(p);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
            p.sendMessage("§a" + eff.getDisplay() + " §f레벨이 §b" + prog.getEffectLevel(eff) + "§f이 되었습니다!");
        } else {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            p.sendMessage("§c업그레이드에 실패했습니다.");
        }

        EffectShopGUI.open(p);
        EffectApplier.applyFor(p);
    }
}
