package me.junick.itemBingo.events.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.gui.BingoGUI;
import me.junick.itemBingo.gui.BingoGuiHolder;
import me.junick.itemBingo.gui.BingoViewport;
import me.junick.itemBingo.model.BingoBoard;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Read-only board preview ({@code /summary board}): every click is cancelled so
 * nothing can be taken out or submitted. The only interactive elements are the
 * scroll arrows / recenter button on an oversized board, which pan the viewport
 * exactly like the live board. There is no back button.
 */
public class BoardPreviewClickEvent implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!BingoGuiHolder.is(e.getView().getTopInventory(), BingoGuiHolder.Gui.BOARD_PREVIEW)) return;

        e.setCancelled(true);

        BingoBoard board = ItemBingo.currentBingo;
        if (board == null || !BingoViewport.needsScroll(board)) return;

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= e.getView().getTopInventory().getSize()) return;

        BingoViewport.Layout layout = BingoViewport.layout(p.getUniqueId(), board);
        BingoViewport.Control ctrl = BingoViewport.controlAt(layout, slot);
        if (ctrl == BingoViewport.Control.NONE) return;

        // A fast double-tap fires a normal click AND a synthetic DOUBLE_CLICK on
        // the same slot; ignore the latter so one tap scrolls exactly one cell.
        if (e.getClick() == ClickType.DOUBLE_CLICK) return;

        switch (ctrl) {
            case UP    -> { if (!layout.upActive())    return; BingoViewport.scroll(p.getUniqueId(), board, -1, 0); }
            case DOWN  -> { if (!layout.downActive())  return; BingoViewport.scroll(p.getUniqueId(), board, 1, 0); }
            case LEFT  -> { if (!layout.leftActive())  return; BingoViewport.scroll(p.getUniqueId(), board, 0, -1); }
            case RIGHT -> { if (!layout.rightActive()) return; BingoViewport.scroll(p.getUniqueId(), board, 0, 1); }
            case RECENTER -> BingoViewport.centerOn(p.getUniqueId(), board);
            default -> { return; }
        }

        BingoGUI.rerenderInPlace(p);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }
}
