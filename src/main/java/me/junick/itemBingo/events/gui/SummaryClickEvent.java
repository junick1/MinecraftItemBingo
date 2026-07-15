package me.junick.itemBingo.events.gui;

import me.junick.itemBingo.gui.BingoGUI;
import me.junick.itemBingo.gui.BingoGuiHolder;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Read-only results screen: every click is cancelled. The close button shuts the
 * screen and the board-view button opens the read-only original-board preview.
 */
public class SummaryClickEvent implements Listener {
    private static final int BOARD_SLOT = 45;
    private static final int CLOSE_SLOT = 53;

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!BingoGuiHolder.is(e.getView().getTopInventory(), BingoGuiHolder.Gui.SUMMARY)) return;

        e.setCancelled(true);

        if (e.getRawSlot() == BOARD_SLOT) {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            BingoGUI.openPreview(p);
        } else if (e.getRawSlot() == CLOSE_SLOT) {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            p.closeInventory();
        }
    }
}
