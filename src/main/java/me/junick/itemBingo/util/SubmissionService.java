package me.junick.itemBingo.util;

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
import me.junick.itemBingo.network.ModSync;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The single home of bingo submission rules: validation (fog submit-lock,
 * lockout claims, item matching) and completion (progress, line awards,
 * GUI refreshes, broadcasts). Used by both the inventory click listener
 * ({@code BingoClickEvent}) and the companion-mod network handler, so the
 * two submission paths can never drift apart.
 */
public final class SubmissionService {
    private SubmissionService() {}

    /** Reveal subtitle fades in instantly, holds, then fades out (matches the map items). */
    private static final Title.Times REVEAL_TIMES =
            Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500));

    /** Outcome of validating a direct (specific-cell) submission attempt. */
    public enum DirectResult {
        OK, OK_FILLER, OUT_OF_RANGE, ALREADY_SUBMITTED, FOG_HIDDEN, LOCKED, INVALID_ITEM, ITEM_MISMATCH
    }

    /**
     * Validates a direct submission of {@code candidate} onto cell {@code idx},
     * in the exact order the board GUI applies its checks: already submitted →
     * fog submit-lock → lockout claim → item validity → filler → type match.
     * Pure check — consumes nothing and completes nothing.
     */
    public static DirectResult validateDirect(Player p, BingoProgressAccess progress,
                                              BingoBoard board, int idx, ItemStack candidate) {
        if (idx < 0 || idx >= board.getItems().size()) return DirectResult.OUT_OF_RANGE;
        if (progress.isSubmitted(idx)) return DirectResult.ALREADY_SUBMITTED;

        // In Fog of War with submit-lock, a still-hidden cell can't be submitted to.
        Set<Integer> revealed = fogLockedRevealed(board, progress);
        if (revealed != null && !revealed.contains(idx)) return DirectResult.FOG_HIDDEN;

        // In Lockout, a cell another team already claimed is off-limits.
        Set<Integer> locked = lockoutBlocked(p);
        if (locked != null && locked.contains(idx)) return DirectResult.LOCKED;

        if (!isValidItem(candidate)) return DirectResult.INVALID_ITEM;

        if (CustomItems.is(candidate, BingoItem.BINGO_FILLER)) return DirectResult.OK_FILLER;

        ItemStack required = board.getItems().get(idx);
        if (!isMatchingItem(candidate, required)) return DirectResult.ITEM_MISMATCH;

        return DirectResult.OK;
    }

    /**
     * The cell a shift-click auto-submit of {@code candidate} would land on:
     * the first unsubmitted, revealed (fog submit-lock), unclaimed (lockout)
     * cell whose required item type matches. {@code -1} when there is none.
     * Filler items never auto-submit (a free cell choice must be deliberate).
     */
    public static int findShiftTarget(Player p, BingoProgressAccess progress,
                                      BingoBoard board, ItemStack candidate) {
        if (!isValidItem(candidate)) return -1;
        if (CustomItems.is(candidate, BingoItem.BINGO_FILLER)) return -1;

        // In Fog of War with submit-lock, auto-submit must not "find" hidden cells.
        Set<Integer> revealed = fogLockedRevealed(board, progress);
        // In Lockout, auto-submit must skip cells another team already claimed.
        Set<Integer> locked = lockoutBlocked(p);

        for (int idx = 0; idx < board.getItems().size(); idx++) {
            if (progress.isSubmitted(idx)) continue;
            if (revealed != null && !revealed.contains(idx)) continue;
            if (locked != null && locked.contains(idx)) continue;

            ItemStack required = board.getItems().get(idx);
            if (!isMatchingItem(candidate, required)) continue;

            return idx;
        }
        return -1;
    }

    /** 제출 완료 처리 (저장, GUI 업데이트, 효과음, 메세지 등) */
    public static void completeSubmission(Player p, BingoProgressAccess progress, BingoBoard board, int idx) {
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

        // Companion-mod clients need the new cell state too (a lockout claim or
        // fog reveal can change every viewer's board, so push to all of them).
        ModSync.broadcastBoard();
    }

    /* ========================= Failure Feedback ========================= */

    /** 다른 팀이 선점한 칸 제출 시도 메세지 */
    public static void sendLockedMessage(Player p) {
        p.sendMessage(Messages.get(p, "board.locked-claimed"));
        playErrorSound(p);
    }

    /** 잘못된 아이템 메세지 */
    public static void sendInvalidItemMessage(Player p) {
        p.sendMessage(Messages.get(p, "board.invalid-item"));
        playErrorSound(p);
    }

    /** 실패 효과음 */
    public static void playErrorSound(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
    }

    /* ========================= Validation Helpers ========================= */

    /**
     * The currently-revealed cells when Fog of War submit-lock is active, or
     * {@code null} when submissions aren't reveal-gated (not fog mode, or the
     * submit-lock setting is off). Callers treat {@code null} as "no restriction".
     */
    private static Set<Integer> fogLockedRevealed(BingoBoard board, BingoProgressAccess progress) {
        if (!Settings.isFogOfWarMode() || !Settings.isFogSubmitLock()) return null;
        return FogOfWar.revealedSlots(
                board.getWidth(), board.getHeight(),
                progress.getSubmittedSlots(), Settings.isFogDiagonalReveal());
    }

    /**
     * In Lockout mode, the cells already claimed by another team — which {@code p}
     * can no longer submit. {@code null} when not in Lockout mode (no restriction).
     */
    private static Set<Integer> lockoutBlocked(Player p) {
        if (!Settings.isLockoutMode()) return null;
        return Lockout.lockedSlots(p);
    }

    /** 아이템이 null이 아니고 공기가 아닌지 확인  */
    private static boolean isValidItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    /** 아이템 종류가 빙고가 요구하는 아이템과 일치하는지 확인 */
    private static boolean isMatchingItem(ItemStack submitted, ItemStack required) {
        return submitted.getType() == required.getType();
    }

    /* ========================= GUI & Feedback ========================= */

    /** GUI의 모든 제출된 슬롯을 갱신 (진행도 막대가 동기화되도록) */
    private static void refreshSubmittedSlots(Player p, BingoBoard board, BingoProgressAccess progress) {
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
                    board.getItems().get(idx).getType(),
                    owner,
                    progress.getSubmitterName(idx),
                    BingoGUI.progressFraction(progress, owner, total),
                    progress.getSubmissionTime(idx),
                    loc
            ));
        }
    }

    /** 제출 피드백 메세지 및 효과음 */
    private static void sendFeedback(Player p, BingoProgressAccess progress, Material item) {
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

        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
    }

    /* ========================= Line Awards ========================= */

    /** 줄 완성 체크 및 보상 */
    private static void checkAndAwardLine(Player p, BingoProgressAccess progress, int width, int height, int justSubmittedIdx) {
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
    private static boolean isLineComplete(BingoProgressAccess progress, int width, int height, int idx, boolean row) {
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
    private static boolean isGeneralDiagonalComplete(BingoProgressAccess progress, int width, int height, int r, int c, boolean main) {
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
