package me.junick.itemBingo.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.model.BingoBoard;
import me.junick.itemBingo.records.ranking.RankingEntry;
import me.junick.itemBingo.util.RankingFormat;
import me.junick.itemBingo.util.RankingProviders;
import me.junick.itemBingo.util.TeamManager;
import me.junick.itemBingo.util.TimerManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Post-game results screen: the final standings of the most recently played
 * board, frozen in a read-only GUI. Available only while the timer is inactive
 * (see {@link #tryOpen}) so it reads as a recap, not a live leaderboard — that
 * also dovetails with the "hide leaderboard during play" option.
 *
 * <p>Standings come from the same {@link RankingProviders} the sidebar uses, so
 * solo/team mode and the penalty system are handled transparently. The viewer's
 * own row is glow-highlighted and mirrored in a personal-result card, so a
 * player who placed outside the visible grid can still see exactly how they did.
 */
public class SummaryGUI {
    public static final String TITLE = "§6게임 결과";

    private static final int SIZE = 54;
    private static final int INFO_SLOT = 4;
    private static final int PERSONAL_SLOT = 49;
    private static final int CLOSE_SLOT = 53;

    /** Standings slots: the inner 7 columns of rows 1–4, leaving a framed border. */
    private static final int[] ENTRY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43,
    };

    /**
     * Opens the results screen, or tells the player why it isn't available yet:
     * no game has been played, or one is still in progress (results unlock when
     * the timer stops). Single entry point shared by {@code /summary} and the
     * menu button.
     */
    public static void tryOpen(Player p) {
        if (ItemBingo.currentBingo == null) {
            p.sendMessage(Component.text("아직 진행된 게임이 없습니다.", NamedTextColor.RED));
            return;
        }
        if (TimerManager.isRunning()) {
            p.sendMessage(Component.text("게임이 끝난 후에 결과를 확인할 수 있습니다.", NamedTextColor.RED));
            return;
        }
        open(p);
    }

    private static void open(Player p) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        fillFrame(inv);

        List<RankingEntry> standings = RankingProviders.current().getRankings(0);
        int myIndex = viewerIndex(standings, p);

        inv.setItem(INFO_SLOT, infoCard(standings));

        for (int i = 0; i < ENTRY_SLOTS.length && i < standings.size(); i++) {
            inv.setItem(ENTRY_SLOTS[i], entryIcon(i + 1, standings.get(i), i == myIndex));
        }

        inv.setItem(PERSONAL_SLOT, personalCard(standings, myIndex));
        inv.setItem(CLOSE_SLOT, closeButton());

        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f);
        p.openInventory(inv);
    }

    /**
     * The viewer's index in the sorted standings, or -1 if they didn't take part
     * (e.g. an OP spectating with no team). Matched on the same display name the
     * provider renders: the player's name in solo mode, the {@code "Team N ("}
     * prefix in team mode.
     */
    private static int viewerIndex(List<RankingEntry> standings, Player p) {
        if (RankingProviders.isTeamMode()) {
            int teamId = ItemBingo.getInstance().getTeamManager().effectiveTeamId(p);
            if (teamId == TeamManager.NO_TEAM) return -1;
            String prefix = "Team " + (teamId + 1) + " (";
            for (int i = 0; i < standings.size(); i++) {
                if (standings.get(i).displayName().startsWith(prefix)) return i;
            }
        } else {
            for (int i = 0; i < standings.size(); i++) {
                if (standings.get(i).displayName().equals(p.getName())) return i;
            }
        }
        return -1;
    }

    /** A single standings row: a medal-ranked icon with score and record in its lore. */
    private static ItemStack entryIcon(int rank, RankingEntry entry, boolean isMe) {
        Material material = switch (rank) {
            case 1 -> Material.GOLD_INGOT;
            case 2 -> Material.IRON_INGOT;
            case 3 -> Material.COPPER_INGOT;
            default -> Material.PAPER;
        };
        String medalColor = switch (rank) {
            case 1 -> "§6";  // gold
            case 2 -> "§7";  // silver
            case 3 -> "§c";  // bronze
            default -> "§f"; // white
        };

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(medalColor + "§l" + rank + "위 §r§f" + entry.displayName())
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(line("§7획득: §a" + entry.score() + "개"));
        lore.add(line("§7기록: §f" + RankingFormat.penalty(entry.penaltySeconds())));
        if (isMe) {
            lore.add(Component.empty());
            lore.add(line("§6▶ 나의 기록"));
            // Glow to mark the viewer's own row at a glance.
            meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /** The header card: game mode, scoring, board size, turnout and the winner. */
    private static ItemStack infoCard(List<RankingEntry> standings) {
        BingoBoard board = ItemBingo.currentBingo;
        int total = board.getItems().size();
        boolean teamMode = RankingProviders.isTeamMode();

        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line("§e§l게임 결과 요약"));

        List<Component> lore = new ArrayList<>();
        lore.add(line("§7모드: §f" + Settings.getGameMode().getDisplay()));
        lore.add(line("§7점수 방식: §f" + Settings.getPenaltyDisplay()));
        lore.add(line("§7빙고판: §f" + board.getWidth() + "×" + board.getHeight() + " §7(총 " + total + "칸)"));
        lore.add(line("§7참가: §f" + standings.size() + (teamMode ? "팀" : "명")));
        lore.add(Component.empty());
        if (!standings.isEmpty() && standings.get(0).score() > 0) {
            RankingEntry winner = standings.get(0);
            lore.add(line("§6우승: §f" + winner.displayName() + " §7(" + winner.score() + "개)"));
        } else {
            lore.add(line("§7우승: 제출한 참가자가 없습니다"));
        }
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /** The viewer's own result, shown even when they placed outside the visible grid. */
    private static ItemStack personalCard(List<RankingEntry> standings, int myIndex) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line("§b§l나의 결과"));

        List<Component> lore = new ArrayList<>();
        if (myIndex >= 0) {
            RankingEntry mine = standings.get(myIndex);
            lore.add(line("§7순위: §e" + (myIndex + 1) + "위 §7/ " + standings.size()));
            lore.add(line("§7획득: §a" + mine.score() + "개"));
            lore.add(line("§7기록: §f" + RankingFormat.penalty(mine.penaltySeconds())));
        } else {
            lore.add(line("§7이 게임에 참가하지 않았습니다."));
        }
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack closeButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line("§c닫기"));
        meta.lore(List.of(line("§7결과 창을 닫습니다.")));
        item.setItemMeta(meta);
        return item;
    }

    private static void fillFrame(Inventory inv) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setHideTooltip(true);
        pane.setItemMeta(meta);
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, pane);
        }
    }

    private static Component line(String legacy) {
        return Component.text(legacy).decoration(TextDecoration.ITALIC, false);
    }
}
