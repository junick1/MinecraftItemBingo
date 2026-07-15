package me.junick.itemBingo.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import me.junick.itemBingo.model.BingoBoard;
import me.junick.itemBingo.records.ranking.RankingEntry;
import me.junick.itemBingo.util.RankingFormat;
import me.junick.itemBingo.util.RankingProviders;
import me.junick.itemBingo.util.TeamManager;
import me.junick.itemBingo.util.TimerManager;
import net.kyori.adventure.text.Component;
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
 * (see {@link #tryOpen}) so it reads as a recap, not a live leaderboard.
 */
public class SummaryGUI {
    /** Title message key. GUI identity is the {@link BingoGuiHolder} marker, not the title. */
    public static final String TITLE_KEY = "gui.summary.title";

    private static final int SIZE = 54;
    private static final int INFO_SLOT = 4;
    private static final int BOARD_SLOT = 45;
    private static final int PERSONAL_SLOT = 49;
    private static final int CLOSE_SLOT = 53;

    /** Standings slots: the inner 7 columns of rows 1–4, leaving a framed border. */
    private static final int[] ENTRY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43,
    };

    public static void tryOpen(Player p) {
        if (ItemBingo.currentBingo == null) {
            p.sendMessage(Messages.get(p, "gui.summary.no-game"));
            return;
        }
        if (TimerManager.isRunning()) {
            p.sendMessage(Messages.get(p, "gui.summary.in-progress"));
            return;
        }
        open(p);
    }

    private static void open(Player p) {
        SupportedLocale loc = Messages.localeOf(p);

        BingoGuiHolder holder = new BingoGuiHolder(BingoGuiHolder.Gui.SUMMARY);
        Inventory inv = Bukkit.createInventory(holder, SIZE, Messages.get(loc, TITLE_KEY));
        holder.setInventory(inv);
        fillFrame(inv);

        List<RankingEntry> standings = RankingProviders.current().getRankings(0);
        int myIndex = viewerIndex(standings, p);

        inv.setItem(INFO_SLOT, infoCard(standings, loc));

        for (int i = 0; i < ENTRY_SLOTS.length && i < standings.size(); i++) {
            inv.setItem(ENTRY_SLOTS[i], entryIcon(i + 1, standings.get(i), i == myIndex, loc));
        }

        inv.setItem(BOARD_SLOT, boardButton(loc));
        inv.setItem(PERSONAL_SLOT, personalCard(standings, myIndex, loc));
        inv.setItem(CLOSE_SLOT, closeButton(loc));

        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f);
        p.openInventory(inv);
    }

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

    private static ItemStack entryIcon(int rank, RankingEntry entry, boolean isMe, SupportedLocale loc) {
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

        meta.displayName(line(medalColor + "§l" + Messages.legacy(loc, "gui.summary.rank-fmt", "rank", rank)
                + " §r§f" + entry.displayName()));

        List<Component> lore = new ArrayList<>();
        lore.add(line(Messages.legacy(loc, "gui.summary.score", "score", entry.score())));
        lore.add(line(Messages.legacy(loc, "gui.summary.record", "penalty", RankingFormat.penalty(entry.penaltySeconds(), loc))));
        if (isMe) {
            lore.add(Component.empty());
            lore.add(line(Messages.legacy(loc, "gui.summary.my-record")));
            meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack infoCard(List<RankingEntry> standings, SupportedLocale loc) {
        BingoBoard board = ItemBingo.currentBingo;
        int total = board.getItems().size();
        boolean teamMode = RankingProviders.isTeamMode();

        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line(Messages.legacy(loc, "gui.summary.info-title")));

        List<Component> lore = new ArrayList<>();
        lore.add(line(Messages.legacy(loc, "gui.summary.mode", "mode", Settings.getGameMode().displayName(loc))));
        lore.add(line(Messages.legacy(loc, "gui.summary.scoring", "scoring", Settings.getPenaltyDisplay(loc))));
        lore.add(line(Messages.legacy(loc, "gui.summary.board", "width", board.getWidth(), "height", board.getHeight(), "total", total)));
        lore.add(line(Messages.legacy(loc, teamMode ? "gui.summary.participants-team" : "gui.summary.participants-solo",
                "count", standings.size())));
        lore.add(Component.empty());
        if (!standings.isEmpty() && standings.get(0).score() > 0) {
            RankingEntry winner = standings.get(0);
            lore.add(line(Messages.legacy(loc, "gui.summary.winner", "name", winner.displayName(), "score", winner.score())));
        } else {
            lore.add(line(Messages.legacy(loc, "gui.summary.no-winner")));
        }
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack personalCard(List<RankingEntry> standings, int myIndex, SupportedLocale loc) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line(Messages.legacy(loc, "gui.summary.personal-title")));

        List<Component> lore = new ArrayList<>();
        if (myIndex >= 0) {
            RankingEntry mine = standings.get(myIndex);
            lore.add(line(Messages.legacy(loc, "gui.summary.my-rank", "rank", myIndex + 1, "total", standings.size())));
            lore.add(line(Messages.legacy(loc, "gui.summary.score", "score", mine.score())));
            lore.add(line(Messages.legacy(loc, "gui.summary.record", "penalty", RankingFormat.penalty(mine.penaltySeconds(), loc))));
        } else {
            lore.add(line(Messages.legacy(loc, "gui.summary.not-participated")));
        }
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack boardButton(SupportedLocale loc) {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line(Messages.legacy(loc, "gui.summary.board-view.name")));
        meta.lore(List.of(line(Messages.legacy(loc, "gui.summary.board-view.lore"))));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack closeButton(SupportedLocale loc) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line(Messages.legacy(loc, "gui.summary.close.name")));
        meta.lore(List.of(line(Messages.legacy(loc, "gui.summary.close.lore"))));
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
