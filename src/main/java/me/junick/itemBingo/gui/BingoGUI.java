package me.junick.itemBingo.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.enums.BingoItemTag;
import me.junick.itemBingo.interfaces.access.BingoProgressAccess;
import me.junick.itemBingo.interfaces.access.SoloProgressAccess;
import me.junick.itemBingo.model.BingoBoard;
import me.junick.itemBingo.model.PlayerBingoProgress;
import me.junick.itemBingo.util.BingoTagLoader;
import me.junick.itemBingo.util.FogOfWar;
import me.junick.itemBingo.util.Lockout;
import me.junick.itemBingo.util.PlayerDataManager;
import me.junick.itemBingo.util.ProgressFactory;
import me.junick.itemBingo.util.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.datacomponent.item.TooltipDisplay;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BingoGUI {
    private static final int GUI_WIDTH = 9;
    private static final int MAX_HEIGHT = 6;

    public static final String TITLE = "§f빙고판";

    /**
     * Whether {@code p} is allowed to view the bingo board. In team mode only
     * team members (and OPs, who may spectate) may see it; outside team mode
     * everyone can. OPs can view but still can't submit — see {@code BingoClickEvent}.
     */
    public static boolean canView(Player p) {
        if (!Settings.isTeamEnabled()) return true;
        if (p.isOp()) return true;
        return ItemBingo.getInstance().getTeamManager().getTeamId(p) != TeamManager.NO_TEAM;
    }

    public static void open(Player p) {
        BingoBoard board = ItemBingo.currentBingo;
        if (board == null) {
            p.sendMessage("§c현재 빙고판이 없습니다.");
            return;
        }

        if (!canView(p)) {
            p.sendMessage(Component.text("팀 모드에서는 팀에 배정된 플레이어만 빙고판을 볼 수 있습니다.", NamedTextColor.RED));
            return;
        }

        BingoProgressAccess progress = ProgressFactory.of(p);

        // Oversized boards (beyond the 9x6 inventory limit) render through the
        // scrollable viewport instead of the centered full-board layout below.
        if (BingoViewport.needsScroll(board)) {
            // Restore the player's last scroll position; on a fresh board (no saved
            // position — cleared by applyNewBoard) start centered on the board.
            if (!BingoViewport.has(p.getUniqueId())) {
                BingoViewport.centerOn(p.getUniqueId(), board);
            }
            Inventory inv = Bukkit.createInventory(null, GUI_WIDTH * MAX_HEIGHT, TITLE);
            renderScroll(inv, board, progress, p);
            p.openInventory(inv);
            return;
        }

        int boardWidth = board.getWidth();
        int boardHeight = board.getHeight();
        boolean tightMode = boardHeight > 4;

        int guiHeight = tightMode ? boardHeight : boardHeight + 2;
        if (guiHeight > MAX_HEIGHT) {
            p.sendMessage(Component.text("GUI 높이가 " + MAX_HEIGHT + "줄을 초과합니다.", NamedTextColor.RED));
            return;
        }

        Inventory inv = Bukkit.createInventory(
                null,
                GUI_WIDTH * guiHeight,
                TITLE
        );

        fillBackground(inv);
        placeBingoItems(inv, board, progress, tightMode, p);

        p.openInventory(inv);
    }

    private static void fillBackground(Inventory inv) {
        ItemStack filler = createPane(Material.GRAY_STAINED_GLASS_PANE, null);

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }

    /**
     * Re-renders every board cell of {@code p}'s currently open bingo board in
     * place (the background panes are left untouched). Used for live updates such
     * as a submission revealing new cells in Fog of War, without reopening the
     * inventory (which would drop the player's cursor item).
     */
    public static void rerenderInPlace(Player p) {
        if (!TITLE.equals(p.getOpenInventory().getTitle())) return;

        BingoBoard board = ItemBingo.currentBingo;
        if (board == null) return;

        BingoProgressAccess progress = ProgressFactory.of(p);
        Inventory inv = p.getOpenInventory().getTopInventory();

        if (BingoViewport.needsScroll(board)) {
            // Re-renders arrows + cells from scratch (cells scroll in/out of view),
            // so a full redraw is needed rather than only repainting board slots.
            renderScroll(inv, board, progress, p);
            return;
        }

        boolean tightMode = board.getHeight() > 4;
        placeBingoItems(inv, board, progress, tightMode, p);
    }

    private static void placeBingoItems(
            Inventory inv,
            BingoBoard board,
            BingoProgressAccess progress,
            boolean tightMode,
            Player viewer
    ) {
        List<ItemStack> items = board.getItems();
        BingoTagLoader tagLoader = ItemBingo.getInstance().getTagLoader();

        int offsetX = (GUI_WIDTH - board.getWidth()) / 2;
        int offsetY = tightMode ? 0 : 1;

        int total = items.size();

        boolean fog = Settings.isFogOfWarMode();
        Set<Integer> revealed = fog
                ? FogOfWar.revealedSlots(board.getWidth(), board.getHeight(),
                        progress.getSubmittedSlots(), Settings.isFogDiagonalReveal())
                : null;

        // In Lockout, cells claimed by another team show as plain barriers.
        Set<Integer> locked = Lockout.lockedSlots(viewer);

        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                int index = y * board.getWidth() + x;
                if (index >= items.size()) continue;

                int slot = (y + offsetY) * GUI_WIDTH + offsetX + x;
                inv.setItem(slot, cellIcon(board, progress, index, fog, revealed, locked, tagLoader, total));
            }
        }
    }

    /**
     * Renders an oversized board through the scrollable viewport: an all-filler
     * base, then the four edge arrows (only the directions that can still scroll —
     * the rest stay filler, i.e. "hidden"), then the visible board cells. Used by
     * both the initial open and every live re-render, so a scroll always lands in a
     * fully consistent state.
     */
    private static void renderScroll(Inventory inv, BingoBoard board, BingoProgressAccess progress, Player viewer) {
        BingoViewport.Layout layout = BingoViewport.layout(viewer.getUniqueId(), board);

        fillBackground(inv);

        if (layout.up())    inv.setItem(BingoViewport.SLOT_UP,    arrow(Component.text("▲ 위로", NamedTextColor.WHITE)));
        if (layout.down())  inv.setItem(BingoViewport.SLOT_DOWN,  arrow(Component.text("▼ 아래로", NamedTextColor.WHITE)));
        if (layout.left())  inv.setItem(BingoViewport.SLOT_LEFT,  arrow(Component.text("◀ 왼쪽", NamedTextColor.WHITE)));
        if (layout.right()) inv.setItem(BingoViewport.SLOT_RIGHT, arrow(Component.text("▶ 오른쪽", NamedTextColor.WHITE)));

        BingoTagLoader tagLoader = ItemBingo.getInstance().getTagLoader();
        int total = board.getItems().size();
        boolean fog = Settings.isFogOfWarMode();
        Set<Integer> revealed = fog
                ? FogOfWar.revealedSlots(board.getWidth(), board.getHeight(),
                        progress.getSubmittedSlots(), Settings.isFogDiagonalReveal())
                : null;
        Set<Integer> locked = Lockout.lockedSlots(viewer);

        for (int index = 0; index < board.getItems().size(); index++) {
            int slot = BingoViewport.indexToSlot(index, board, layout);
            if (slot < 0) continue;
            inv.setItem(slot, cellIcon(board, progress, index, fog, revealed, locked, tagLoader, total));
        }
    }

    /**
     * The icon for a single board cell, shared by the centered layout and the
     * scrollable viewport so a cell looks identical in either: a submitted icon,
     * a Lockout barrier, a Fog of War placeholder, or the required item itself.
     */
    private static ItemStack cellIcon(
            BingoBoard board,
            BingoProgressAccess progress,
            int index,
            boolean fog,
            Set<Integer> revealed,
            Set<Integer> locked,
            BingoTagLoader tagLoader,
            int total
    ) {
        if (progress.isSubmitted(index)) {
            UUID owner = progress.getSubmitterId(index);
            return submittedIcon(owner, progress.getSubmitterName(index), progressFraction(progress, owner, total));
        } else if (locked.contains(index)) {
            return lockedIcon();
        } else if (fog && !revealed.contains(index)) {
            return hiddenCell();
        } else {
            return withTagLore(board.getItems().get(index), tagLoader);
        }
    }

    /** A scroll-arrow button (one per live edge); cosmetic only — handled by slot in the click listener. */
    private static ItemStack arrow(Component name) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("클릭하여 한 칸 스크롤", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * The placeholder shown for a not-yet-revealed Fog of War cell: a glowing
     * light gray pane (contrasting the gray outline) with its tooltip hidden so
     * players can't tell what item the cell wants.
     */
    private static ItemStack hiddenCell() {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setHideTooltip(true);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * The icon shown for a Lockout cell already claimed by another team: a plain
     * barrier with no durability bar (unlike the submitted-cell icon, which is a
     * netherite axe styled as a barrier whose durability tracks progress). It just
     * signals "another team got here first — this cell is gone".
     */
    private static ItemStack lockedIcon() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("✘ 선점됨", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("다른 팀이 먼저 제출한 칸입니다.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("더 이상 제출할 수 없습니다.", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Progress fraction for a submitted slot's durability bar: a team submitter's
     * personal contribution, or the solo player's overall completion (no owner).
     */
    public static double progressFraction(BingoProgressAccess progress, UUID owner, int total) {
        if (total == 0) return 0.0;
        int submitted = (owner == null)
                ? progress.getSubmittedSlots().size()
                : progress.getSubmissionCount(owner);
        return (double) submitted / total;
    }

    /**
     * The icon shown for an already-submitted slot, used by both the initial
     * render and live updates so a submitted slot always looks the same.
     *
     * <p>The base item is a Netherite Axe whose {@code item_model} is swapped to a
     * player head — in a team match, with the submitter's {@code profile} so the
     * head shows their skin — or to a barrier (solo, no profile). Either way the
     * axe's durability bar is scaled to {@code fraction}, so a fuller (greener) bar
     * means more progress: in a team match that submitter's personal contribution,
     * and on a solo board the player's overall completion.
     *
     * @param submitterId   submitter UUID, or {@code null} for the barrier variant
     * @param submitterName submitter name shown in the lore (team match only)
     * @param fraction      progress fraction in [0, 1] driving the durability bar
     */
    public static ItemStack submittedIcon(UUID submitterId, String submitterName, double fraction) {
        boolean teamMatch = submitterId != null;

        ItemStack item = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta meta = item.getItemMeta();

        meta.setItemModel(NamespacedKey.minecraft(teamMatch ? "player_head" : "barrier"));

        meta.displayName(Component.text("✔ 제출됨", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("이미 제출한 칸입니다.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        if (teamMatch) {
            lore.add(Component.text("제출자: ", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(
                            submitterName != null ? submitterName : "알 수 없음",
                            NamedTextColor.YELLOW)));
        }
        meta.lore(lore);

        // The durability bar shows progress: remaining durability in [1, max].
        if (meta instanceof Damageable dmg) {
            int max = Material.NETHERITE_AXE.getMaxDurability();
            double clamped = Math.max(0.0, Math.min(1.0, fraction));
            int remaining = Math.max(1, Math.min(max, (int) Math.round(clamped * max)));
            dmg.setDamage(max - remaining);
        }

        item.setItemMeta(meta);

        // Keep the durability bar but hide the axe's attribute lines and the raw
        // "Durability: x / y" tooltip text, which would only be noise here.
        TooltipDisplay.Builder tooltip = TooltipDisplay.tooltipDisplay()
                .addHiddenComponents(DataComponentTypes.ATTRIBUTE_MODIFIERS, DataComponentTypes.DAMAGE);
        if (teamMatch) {
            // The profile is only there to skin the head, so hide it from the tooltip.
            tooltip.addHiddenComponents(DataComponentTypes.PROFILE);
        }
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, tooltip.build());

        if (teamMatch) {
            item.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile()
                    .uuid(submitterId)
                    .build());
        }

        return item;
    }

    private static ItemStack createPane(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (name != null) {
            meta.displayName(name);
        } else {
            meta.setHideTooltip(true);
        }

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack withTagLore(ItemStack original, BingoTagLoader tagLoader) {
        ItemStack item = original.clone();
        ItemMeta meta = item.getItemMeta();

        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_DYE,
                ItemFlag.HIDE_ARMOR_TRIM
        );

        EnumSet<BingoItemTag> tags = tagLoader.getTags(item.getType());
        if (tags.isEmpty()) return item;

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        for (BingoItemTag tag : tags) {
            lore.add(tag.bullet());
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
