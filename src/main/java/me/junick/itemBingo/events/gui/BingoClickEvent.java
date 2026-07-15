package me.junick.itemBingo.events.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.enums.BingoItem;
import me.junick.itemBingo.enums.BingoRewardType;
import me.junick.itemBingo.gui.BingoGUI;
import me.junick.itemBingo.gui.BingoGuiHolder;
import me.junick.itemBingo.gui.BingoViewport;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import me.junick.itemBingo.interfaces.access.BingoProgressAccess;
import me.junick.itemBingo.model.BingoBoard;
import me.junick.itemBingo.model.PlayerBingoProgress;
import me.junick.itemBingo.model.TeamBingoProgress;
import me.junick.itemBingo.util.*;
import me.junick.itemBingo.webserver.WebUIServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.naming.Name;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BingoClickEvent implements Listener {
    /** Reveal subtitle fades in instantly, holds, then fades out (matches the map items). */
    private static final Title.Times REVEAL_TIMES =
            Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500));

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
        boolean teamPlay = (teamId != TeamManager.NO_TEAM);

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
        if (!isValidItem(clicked)) return;

        if (CustomItems.is(clicked, BingoItem.BINGO_FILLER)) {
            return;
        }

        // In Fog of War with submit-lock, auto-submit must not "find" hidden cells.
        Set<Integer> revealed = fogLockedRevealed(board, progress);
        // In Lockout, auto-submit must skip cells another team already claimed.
        Set<Integer> locked = lockoutBlocked(p);

        for (int idx = 0; idx < board.getItems().size(); idx++) {
            if (progress.isSubmitted(idx)) continue;
            if (revealed != null && !revealed.contains(idx)) continue;
            if (locked != null && locked.contains(idx)) continue;

            ItemStack required = board.getItems().get(idx);
            if (!isMatchingItem(clicked, required)) continue;

            consumeItem(e, clicked);
            completeSubmission(p, progress, board, idx);

            // Auto-submit can land on a cell the player can't currently see, so
            // re-center the viewport on it (edges push it as close to center as
            // possible). completeSubmission already re-rendered, so just refocus.
            if (BingoViewport.needsScroll(board)) {
                BingoViewport.focusOn(p.getUniqueId(), board, idx);
                BingoGUI.rerenderInPlace(p);
            }
            return;
        }
    }

    private void handleDirectSubmission(InventoryClickEvent e, Player p, BingoBoard board, BingoProgressAccess progress, int slot) {
        int idx = getBingoIndexFromSlot(slot, board, p);
        if (idx == -1 || progress.isSubmitted(idx)) return;

        // In Fog of War with submit-lock, a still-hidden cell can't be submitted to.
        Set<Integer> revealed = fogLockedRevealed(board, progress);
        if (revealed != null && !revealed.contains(idx)) return;

        // In Lockout, a cell another team already claimed is off-limits.
        Set<Integer> locked = lockoutBlocked(p);
        if (locked != null && locked.contains(idx)) {
            sendLockedMessage(p);
            return;
        }

        ItemStack submitted = e.getCursor();
        if (!isValidItem(submitted)) return;

        if (CustomItems.is(submitted, BingoItem.BINGO_FILLER)) {
            consumeItem(e, submitted);
            completeSubmission(p, progress, board, idx);
            return;
        }

        ItemStack required = board.getItems().get(idx);
        if (!isMatchingItem(submitted, required)) {
            sendInvalidItemMessage(p);
            return;
        }

        consumeItem(e, submitted);
        completeSubmission(p, progress, board, idx);
    }

    /* ========================= Helper Methods ========================= */

    /**
     * The currently-revealed cells when Fog of War submit-lock is active, or
     * {@code null} when submissions aren't reveal-gated (not fog mode, or the
     * submit-lock setting is off). Callers treat {@code null} as "no restriction".
     */
    private Set<Integer> fogLockedRevealed(BingoBoard board, BingoProgressAccess progress) {
        if (!Settings.isFogOfWarMode() || !Settings.isFogSubmitLock()) return null;
        return FogOfWar.revealedSlots(
                board.getWidth(), board.getHeight(),
                progress.getSubmittedSlots(), Settings.isFogDiagonalReveal());
    }

    /**
     * In Lockout mode, the cells already claimed by another team — which {@code p}
     * can no longer submit. {@code null} when not in Lockout mode (no restriction).
     */
    private Set<Integer> lockoutBlocked(Player p) {
        if (!Settings.isLockoutMode()) return null;
        return Lockout.lockedSlots(p);
    }

    /** 다른 팀이 선점한 칸 제출 시도 메세지 */
    private void sendLockedMessage(Player p) {
        p.sendMessage(Messages.get(p, "board.locked-claimed"));
        playErrorSound(p);
    }

    /** 아이템이 null이 아니고 공기가 아닌지 확인  */
    private boolean isValidItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    /** 아이템 종류가 빙고가 요구하는 아이템과 일치하는지 확인 */
    private boolean isMatchingItem(ItemStack submitted, ItemStack required) {
        return submitted.getType() == required.getType();
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

    /** 제출 완료 처리 (저장, GUI 업데이트, 효과음, 메세지 등) */
    private void completeSubmission(Player p, BingoProgressAccess progress, BingoBoard board, int idx) {
        boolean fog = Settings.isFogOfWarMode();

        // Snapshot the revealed cells before submitting so we can tell how many
        // new cells this submission uncovers (for the reveal alert).
        Set<Integer> revealedBefore = fog
                ? FogOfWar.revealedSlots(board.getWidth(), board.getHeight(),
                        progress.getSubmittedSlots(), Settings.isFogDiagonalReveal())
                : null;

        progress.submit(idx, p);

        progress.addCurrencyAll(BingoRewardType.SLOT, 1);

        checkAndAwardLine(p, progress, board.getWidth(), board.getHeight(), idx);
        progress.save();

        int newlyRevealed = 0;
        if (fog) {
            Set<Integer> after = FogOfWar.revealedSlots(board.getWidth(), board.getHeight(),
                    progress.getSubmittedSlots(), Settings.isFogDiagonalReveal());
            after.removeAll(revealedBefore);
            newlyRevealed = after.size();
        }

        if (Settings.isLockoutMode()) {
            // The claim locks this cell out for every other team, so re-render every
            // open board (not just the submitter's team) to show the new barrier.
            GuiSync.forEachViewer(BingoGuiHolder.Gui.BINGO, BingoGUI::rerenderInPlace);
        } else {
            boolean scroll = BingoViewport.needsScroll(board);
            for (Player viewer : progress.viewers(p)) {
                if (fog || scroll) {
                    // A submission can reveal neighbouring cells (fog), and a scrolling
                    // board only ever shows part of the grid, so re-render the whole
                    // viewport rather than trying to repaint individual board slots.
                    BingoGUI.rerenderInPlace(viewer);
                } else {
                    // Every submitted icon's durability bar reflects overall completion,
                    // which just changed, so refresh all of them (not only the new slot).
                    refreshSubmittedSlots(viewer, board, progress);
                }
            }
        }

        if (fog && Settings.isFogRevealAlert() && newlyRevealed > 0) {
            // Center-screen subtitle (like the map items), leaving the action bar
            // free for the timer. Built per viewer so each sees it in their language.
            for (Player viewer : progress.viewers(p)) {
                Title revealTitle = Title.title(
                        Component.empty(),
                        Messages.get(viewer, "board.fog-revealed", "count", newlyRevealed),
                        REVEAL_TIMES
                );
                viewer.showTitle(revealTitle);
                viewer.playSound(viewer.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
            }
        }

        // Submission awards SLOT to every teammate (and possibly LINE to the team),
        // so refresh any teammate who has a shop open to show the new balance.
        GuiSync.refreshShops(progress.viewers(p));

        sendFeedback(p, progress, board.getItems().get(idx).getType());
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

    /** 잘못된 아이템 메세지 */
    private void sendInvalidItemMessage(Player p) {
        p.sendMessage(Messages.get(p, "board.invalid-item"));
        playErrorSound(p);
    }

    /** 실패 효과음 */
    private void playErrorSound(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
    }

    /* ========================= GUI & Feedback ========================= */

    /** GUI의 모든 제출된 슬롯을 갱신 (진행도 막대가 동기화되도록) */
    private void refreshSubmittedSlots(Player p, BingoBoard board, BingoProgressAccess progress) {
        if (!BingoGuiHolder.is(p.getOpenInventory().getTopInventory(), BingoGuiHolder.Gui.BINGO)) return;

        SupportedLocale loc = Messages.localeOf(p);
        Inventory inv = p.getOpenInventory().getTopInventory();
        int width = board.getWidth();
        int height = board.getHeight();
        boolean tight = height > 4;

        int offsetX = (9 - width) / 2;
        int offsetY = tight ? 0 : 1;

        int total = board.getItems().size();

        for (int idx : progress.getSubmittedSlots()) {
            int row = idx / width + offsetY;
            int col = idx % width + offsetX;
            int slot = row * 9 + col;

            UUID owner = progress.getSubmitterId(idx);
            inv.setItem(slot, BingoGUI.submittedIcon(
                    owner,
                    progress.getSubmitterName(idx),
                    BingoGUI.progressFraction(progress, owner, total),
                    loc
            ));
        }
    }

    /** 제출 피드백 메세지 및 효과음 */
    private void sendFeedback(Player p, BingoProgressAccess progress) {
        int current = progress.getSubmittedSlots().size();
        int total = ItemBingo.currentBingo.getItems().size();

        for (Player pl : Bukkit.getOnlinePlayers()) {
            pl.sendMessage(Messages.get(pl, "board.submit-broadcast",
                    "player", p.getName(), "current", current, "total", total));
        }

        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
    }

    private void sendFeedback(Player p, BingoProgressAccess progress, Material item) {
        int current = progress.getSubmittedSlots().size();
        int total = ItemBingo.currentBingo.getItems().size();

        var tm = ItemBingo.getInstance().getTeamManager();
        int teamId = tm.effectiveTeamId(p);
        List<Player> team = (teamId != TeamManager.NO_TEAM)
                ? tm.getOnlinePlayersOnTeam(teamId)
                : List.of(p);

        Component itemInfo = Component.text(" -> ", NamedTextColor.WHITE).append(
                Component.translatable(item.translationKey())
        );

        if (Leaderboard.isHidden()) {
            // The live leaderboard is concealed, so a public "(n/total)" broadcast would
            // leak progress to opponents and defeat the hide. Keep it within the player's
            // own team (or just the player when solo), with the item detail they'd
            // normally get as teammates.
            for (Player member : team) {
                member.sendMessage(Messages.get(member, "board.submit-broadcast",
                        "player", p.getName(), "current", current, "total", total).append(itemInfo));
            }
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Component base = Messages.get(player, "board.submit-broadcast",
                        "player", p.getName(), "current", current, "total", total);
                if (team.contains(player)) {
                    player.sendMessage(base.append(itemInfo));
                } else {
                    player.sendMessage(base);
                }
            }
        }
        
        for (Player member : team) {
            WebUIServer.sendBingoBoard(member);
        }

        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
    }

    /** 줄 완성 체크 및 보상 */
    private void checkAndAwardLine(Player p, BingoProgressAccess progress, int width, int height, int justSubmittedIdx) {
        int r = justSubmittedIdx / width;
        int c = justSubmittedIdx % width;

        boolean rowDone = isLineComplete(progress, width, height, r, true);
        boolean colDone = isLineComplete(progress, width, height, c, false);
        boolean diag1Done = isGeneralDiagonalComplete(progress, width, height, r, c, true);
        boolean diag2Done = isGeneralDiagonalComplete(progress, width, height, r, c, false);

        int cnt = (rowDone ? 1 : 0) + (colDone ? 1 : 0)
                + (diag1Done ? 1 : 0)
                + (diag2Done ? 1 : 0);

        if (cnt > 0) {
            p.sendMessage(Messages.get(p, "board.line-points", "count", cnt));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.33f, 1.0f);
            progress.addCurrencyAll(BingoRewardType.LINE, cnt);
            progress.save();
        }
    }

    /** 특정 행/열이 모두 제출되었는지 확인 */
    private boolean isLineComplete(BingoProgressAccess progress, int width, int height, int idx, boolean row) {
        if (row) {
            for (int i = 0; i < width; i++) {
                int newIdx = idx * width + i;
                if (!progress.isSubmitted(newIdx)) return false;
            }
        } else {
            for (int i = 0; i < height; i++) {
                int newIdx = i * width + idx;
                if (!progress.isSubmitted(newIdx)) return false;
            }
        }
        return true;
    }

    /** 대각선이 모두 제출되었는지 확인 */
    private boolean isGeneralDiagonalComplete(BingoProgressAccess progress, int width, int height, int r, int c, boolean main) {
        int startR, startC;
        if (main) {
            int diff = r - c;

            startR = Math.max(0, diff);
            startC = Math.max(0, -diff);

            int len = Math.min(height - startR, width - startC);
            if (len != Math.min(width, height)) return false;

            while (startR < height && startC < width) {
                int idx = startR * width + startC;
                if (!progress.isSubmitted(idx)) return false;
                startR++;
                startC++;
            }
        } else {
            int sum = r + c;

            startR = Math.max(0, sum - (width - 1));
            startC = Math.min(sum, width - 1);

            int len = Math.min(height - startR, startC + 1);
            if (len != Math.min(width, height)) return false;

            while (startR < height && startC >= 0) {
                int idx = startR * width + startC;
                if (!progress.isSubmitted(idx)) return false;
                startR++;
                startC--;
            }
        }
        return true;
    }
}
