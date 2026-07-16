package me.junick.itemBingo.events.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.gui.BingoGUI;
import me.junick.itemBingo.gui.BingoGuiHolder;
import me.junick.itemBingo.gui.BingoViewport;
import me.junick.itemBingo.interfaces.access.BingoProgressAccess;
import me.junick.itemBingo.model.BingoBoard;
import me.junick.itemBingo.util.ProgressFactory;
import me.junick.itemBingo.util.SubmissionService;
import me.junick.itemBingo.util.TeamManager;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Click handling for the live bingo board GUI. The inventory mechanics
 * (routing, viewport controls, consuming from cursor/slot) live here; the
 * submission rules themselves live in {@link SubmissionService}, shared with
 * the companion-mod network path.
 */
public class BingoClickEvent implements Listener {

    @EventHandler
    public void onBingoClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        if (e.getClick() == ClickType.MIDDLE && p.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        if (!BingoGuiHolder.is(e.getView().getTopInventory(), BingoGuiHolder.Gui.BINGO)) return;

        BingoBoard board = ItemBingo.currentBingo;
        if (board == null) return;

        TeamManager tm = ItemBingo.getInstance().getTeamManager();
        int teamId = tm.getTeamId(p);

        // In team mode only assigned players may submit. OPs can still open the
        // board to spectate, but their interactions are cancelled without effect.
        if (Settings.isTeamEnabled() && teamId == TeamManager.NO_TEAM) {
            e.setCancelled(true);
            return;
        }

        BingoProgressAccess progress = ProgressFactory.of(p);

        int slot = e.getRawSlot();
        int topSize = e.getView().getTopInventory().getSize();

        if (slot >= topSize) {
            handleShiftSubmission(e, p, board, progress);
            return;
        }

        e.setCancelled(true);

        // Oversized boards render in a scrollable viewport; its control slots
        // (scroll arrows + recenter) pan the view rather than submitting anything.
        if (BingoViewport.needsScroll(board)) {
            BingoViewport.Layout layout = BingoViewport.layout(p.getUniqueId(), board);
            BingoViewport.Control ctrl = BingoViewport.controlAt(layout, slot);
            if (ctrl != BingoViewport.Control.NONE) {
                // A fast double-tap fires a normal click AND a synthetic DOUBLE_CLICK on
                // the same slot; ignore the latter so one tap scrolls exactly one cell.
                if (e.getClick() != ClickType.DOUBLE_CLICK) {
                    handleControlClick(p, board, layout, ctrl);
                }
                return; // control slots never submit
            }
        }

        handleDirectSubmission(e, p, board, progress, slot);
    }

    /** Pans (or recenters) the viewport; a hidden/edge arrow is a no-op. */
    private void handleControlClick(Player p, BingoBoard board, BingoViewport.Layout layout, BingoViewport.Control ctrl) {
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

    private void handleShiftSubmission(InventoryClickEvent e, Player p, BingoBoard board, BingoProgressAccess progress) {
        if (!e.isShiftClick()) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked != null && !clicked.getType().isAir() && !SubmissionService.isSubmissionOpen()) {
            SubmissionService.sendGameNotRunningMessage(p);
            return;
        }
        int idx = SubmissionService.findShiftTarget(p, progress, board, clicked);
        if (idx < 0) return;

        consumeItem(e, clicked);
        SubmissionService.completeSubmission(p, progress, board, idx);

        // Auto-submit can land on a cell the player can't currently see, so
        // re-center the viewport on it (edges push it as close to center as
        // possible). completeSubmission already re-rendered, so just refocus.
        if (BingoViewport.needsScroll(board)) {
            BingoViewport.focusOn(p.getUniqueId(), board, idx);
            BingoGUI.rerenderInPlace(p);
        }
    }

    private void handleDirectSubmission(InventoryClickEvent e, Player p, BingoBoard board, BingoProgressAccess progress, int slot) {
        int idx = getBingoIndexFromSlot(slot, board, p);
        if (idx == -1) return;

        ItemStack submitted = e.getCursor();
        switch (SubmissionService.validateDirect(p, progress, board, idx, submitted)) {
            case OK, OK_FILLER -> {
                consumeItem(e, submitted);
                SubmissionService.completeSubmission(p, progress, board, idx);
            }
            case LOCKED -> SubmissionService.sendLockedMessage(p);
            case ITEM_MISMATCH -> SubmissionService.sendInvalidItemMessage(p);
            case GAME_NOT_RUNNING -> {
                // Only nag when the player was actually trying to place an item.
                if (submitted != null && !submitted.getType().isAir()) {
                    SubmissionService.sendGameNotRunningMessage(p);
                }
            }
            default -> {
                // Already submitted, fog-hidden, or empty cursor: silently ignored,
                // matching the pre-refactor behavior.
            }
        }
    }

    /** 인벤토리/커서에서 아이템 1개 소비 */
    private void consumeItem(InventoryClickEvent e, ItemStack item) {
        item.setAmount(item.getAmount() - 1);
        if (e.isShiftClick()) {
            e.setCurrentItem(item.getAmount() <= 0 ? null : item);
        } else {
            e.getWhoClicked().setItemOnCursor(item.getAmount() <= 0 ? null : item);
        }
    }

    /** 슬롯을 빙고판 인덱스로 변환 */
    private int getBingoIndexFromSlot(int slot, BingoBoard board, Player p) {
        // Oversized boards map slots through the player's current scroll position.
        if (BingoViewport.needsScroll(board)) {
            return BingoViewport.slotToIndex(slot, board, BingoViewport.layout(p.getUniqueId(), board));
        }

        int width = board.getWidth();
        int height = board.getHeight();
        boolean tight = height > 4;

        int offsetX = (9 - width) / 2;
        int offsetY = tight ? 0 : 1;

        int row = slot / 9 - offsetY;
        int col = slot % 9 - offsetX;

        if (row < 0 || row >= height || col < 0 || col >= width) return -1;
        return row * width + col;
    }
}
