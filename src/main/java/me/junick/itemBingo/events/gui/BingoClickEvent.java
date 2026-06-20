package me.junick.itemBingo.events.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.enums.BingoItem;
import me.junick.itemBingo.enums.BingoRewardType;
import me.junick.itemBingo.gui.BingoGUI;
import me.junick.itemBingo.interfaces.access.BingoProgressAccess;
import me.junick.itemBingo.model.BingoBoard;
import me.junick.itemBingo.model.PlayerBingoProgress;
import me.junick.itemBingo.model.TeamBingoProgress;
import me.junick.itemBingo.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
import org.bukkit.inventory.meta.ItemMeta;

import javax.naming.Name;
import java.util.ArrayList;
import java.util.List;

public class BingoClickEvent implements Listener {
    @EventHandler
    public void onBingoClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        if (e.getClick() == ClickType.MIDDLE && p.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        Component title = e.getView().title();
        String legacyTitle = LegacyComponentSerializer.legacySection().serialize(title);
        if (!legacyTitle.equals(BingoGUI.TITLE)) return;

        BingoBoard board = ItemBingo.currentBingo;
        if (board == null) return;

        TeamManager tm = ItemBingo.getInstance().getTeamManager();
        int teamId = tm.getTeamId(p);
        boolean teamPlay = (teamId != TeamManager.NO_TEAM);

        BingoProgressAccess progress = ProgressFactory.of(p);

        int slot = e.getRawSlot();
        int topSize = e.getView().getTopInventory().getSize();

        if (slot >= topSize) {
            handleShiftSubmission(e, p, board, progress);
            return;
        }

        e.setCancelled(true);
        handleDirectSubmission(e, p, board, progress, slot);
    }

    private void handleShiftSubmission(InventoryClickEvent e, Player p, BingoBoard board, BingoProgressAccess progress) {
        if (!e.isShiftClick()) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (!isValidItem(clicked)) return;

        if (CustomItems.is(clicked, BingoItem.BINGO_FILLER)) {
            return;
        }

        for (int idx = 0; idx < board.getItems().size(); idx++) {
            if (progress.isSubmitted(idx)) continue;

            ItemStack required = board.getItems().get(idx);
            if (!isMatchingItem(clicked, required)) continue;

            consumeItem(e, clicked);
            completeSubmission(p, progress, board, idx);
            return;
        }
    }

    private void handleDirectSubmission(InventoryClickEvent e, Player p, BingoBoard board, BingoProgressAccess progress, int slot) {
        int idx = getBingoIndexFromSlot(slot, board);
        if (idx == -1 || progress.isSubmitted(idx)) return;

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
        progress.submit(idx);
        progress.addCurrencyAll(BingoRewardType.SLOT, 1);

        checkAndAwardLine(p, progress, board.getWidth(), board.getHeight(), idx);
        progress.save();

        for (Player viewer : progress.viewers(p)) {
            updateGUISlot(viewer, board, idx);
        }

        sendFeedback(p, progress, board.getItems().get(idx).getType());
    }

    /** 슬롯을 빙고판 인덱스로 변환 */
    private int getBingoIndexFromSlot(int slot, BingoBoard board) {
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
        p.sendMessage("§c잘못된 아이템입니다!");
        playErrorSound(p);
    }

    /** 실패 효과음 */
    private void playErrorSound(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
    }

    /* ========================= GUI & Feedback ========================= */

    /** GUI의 해당 슬롯을 제출된 것으로 업데이트 */
    private void updateGUISlot(Player p, BingoBoard board, int idx) {
        String title = LegacyComponentSerializer.legacySection().serialize(p.getOpenInventory().title());
        if (!title.equals(BingoGUI.TITLE)) return;

        Inventory inv = p.getOpenInventory().getTopInventory();
        int width = board.getWidth();
        int height = board.getHeight();
        boolean tight = height > 4;

        int offsetX = (9 - width) / 2;
        int offsetY = tight ? 0 : 1;

        int row = idx / width + offsetY;
        int col = idx % width + offsetX;
        int slot = row * 9 + col;

        ItemStack green = new ItemStack(Material.BARRIER);
        ItemMeta meta = green.getItemMeta();
        meta.displayName(Component.text("§a제출됨"));
        green.setItemMeta(meta);
        inv.setItem(slot, green);
    }

    /** 제출 피드백 메세지 및 효과음 */
    private void sendFeedback(Player p, BingoProgressAccess progress) {
        int current = progress.getSubmittedSlots().size();
        int total = ItemBingo.currentBingo.getItems().size();

        String message = "§a" + p.getName() + "님이 아이템을 제출했습니다! (" + current + "/" + total + ")";
        Bukkit.getOnlinePlayers().forEach(pl -> pl.sendMessage(message));

        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
    }

    private void sendFeedback(Player p, BingoProgressAccess progress, Material item) {
        int current = progress.getSubmittedSlots().size();
        int total = ItemBingo.currentBingo.getItems().size();

        var tm = ItemBingo.getInstance().getTeamManager();
        int teamId = tm.getTeamId(p);
        List<Player> team = (teamId != TeamManager.NO_TEAM)
                ? tm.getOnlinePlayersOnTeam(teamId)
                : List.of(p);

        String rawMessage = p.getName() + "님이 아이템을 제출했습니다! (" + current + "/" + total + ")";
        Component message = Component.text(rawMessage, NamedTextColor.GREEN);
        Component itemInfo = Component.text(" -> ", NamedTextColor.WHITE).append(
                Component.translatable(item.translationKey())
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (team.contains(player)) {
                player.sendMessage(message.append(itemInfo));
            } else {
                player.sendMessage(message);
            }
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
            p.sendMessage("§e팀 빙고줄 포인트 + " + cnt);
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
