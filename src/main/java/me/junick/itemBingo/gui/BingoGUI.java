package me.junick.itemBingo.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.enums.BingoItemTag;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import me.junick.itemBingo.interfaces.access.BingoProgressAccess;
import me.junick.itemBingo.model.BingoBoard;
import me.junick.itemBingo.util.BingoTagLoader;
import me.junick.itemBingo.util.FogOfWar;
import me.junick.itemBingo.util.Lockout;
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

    /** Title message key. GUI identity is the {@link BingoGuiHolder} marker, not the title. */
    public static final String TITLE_KEY = "gui.bingo.title";

    /** Title for the read-only board preview (the original board, no progress overlay). */
    public static final String PREVIEW_TITLE_KEY = "gui.board-preview.title";

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

    /** Opens the live, interactive bingo board. */
    public static void open(Player p) {
        openInternal(p, false);
    }

    /**
     * Opens a read-only preview of the original board — every cell shown as its
     * required item, with no submission/fog/lockout overlay and no way to submit.
     * Oversized boards still scroll exactly like the live board.
     */
    public static void openPreview(Player p) {
        openInternal(p, true);
    }

    private static void openInternal(Player p, boolean preview) {
        BingoBoard board = ItemBingo.currentBingo;
        if (board == null) {
            p.sendMessage(Messages.get(p, "board.none"));
            return;
        }

        // The preview is a post-game recap of the shared board, so it isn't gated
        // by team-view rules the way the live board is.
        if (!preview && !canView(p)) {
            p.sendMessage(Messages.get(p, "board.team-only"));
            return;
        }

        SupportedLocale loc = Messages.localeOf(p);
        BingoProgressAccess progress = ProgressFactory.of(p);

        BingoGuiHolder.Gui type = preview ? BingoGuiHolder.Gui.BOARD_PREVIEW : BingoGuiHolder.Gui.BINGO;
        String titleKey = preview ? PREVIEW_TITLE_KEY : TITLE_KEY;

        // Oversized boards (beyond the 9x6 inventory limit) render through the
        // scrollable viewport instead of the centered full-board layout below.
        if (BingoViewport.needsScroll(board)) {
            // Restore the player's last scroll position; on a fresh board (no saved
            // position — cleared by applyNewBoard) start centered on the board.
            if (!BingoViewport.has(p.getUniqueId())) {
                BingoViewport.centerOn(p.getUniqueId(), board);
            }
            // The adaptive viewport may use fewer than 6 rows (e.g. a short, wide board).
            int guiRows = BingoViewport.layout(p.getUniqueId(), board).guiRows();
            BingoGuiHolder holder = new BingoGuiHolder(type);
            Inventory inv = Bukkit.createInventory(holder, GUI_WIDTH * guiRows, Messages.get(loc, titleKey));
            holder.setInventory(inv);
            renderScroll(inv, board, progress, p, loc, preview);
            p.openInventory(inv);
            return;
        }

        int boardHeight = board.getHeight();
        boolean tightMode = boardHeight > 4;

        int guiHeight = tightMode ? boardHeight : boardHeight + 2;
        if (guiHeight > MAX_HEIGHT) {
            p.sendMessage(Messages.get(p, "board.too-tall", "max", MAX_HEIGHT));
            return;
        }

        BingoGuiHolder holder = new BingoGuiHolder(type);
        Inventory inv = Bukkit.createInventory(holder, GUI_WIDTH * guiHeight, Messages.get(loc, titleKey));
        holder.setInventory(inv);

        fillBackground(inv);
        placeBingoItems(inv, board, progress, tightMode, p, loc, preview);

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
        Inventory inv = p.getOpenInventory().getTopInventory();
        boolean preview;
        if (BingoGuiHolder.is(inv, BingoGuiHolder.Gui.BINGO)) {
            preview = false;
        } else if (BingoGuiHolder.is(inv, BingoGuiHolder.Gui.BOARD_PREVIEW)) {
            preview = true;
        } else {
            return;
        }

        BingoBoard board = ItemBingo.currentBingo;
        if (board == null) return;

        SupportedLocale loc = Messages.localeOf(p);
        BingoProgressAccess progress = ProgressFactory.of(p);

        if (BingoViewport.needsScroll(board)) {
            // Re-renders arrows + cells from scratch (cells scroll in/out of view),
            // so a full redraw is needed rather than only repainting board slots.
            renderScroll(inv, board, progress, p, loc, preview);
            return;
        }

        boolean tightMode = board.getHeight() > 4;
        placeBingoItems(inv, board, progress, tightMode, p, loc, preview);
    }

    private static void placeBingoItems(
            Inventory inv,
            BingoBoard board,
            BingoProgressAccess progress,
            boolean tightMode,
            Player viewer,
            SupportedLocale loc,
            boolean preview
    ) {
        List<ItemStack> items = board.getItems();
        BingoTagLoader tagLoader = ItemBingo.getInstance().getTagLoader();

        int offsetX = (GUI_WIDTH - board.getWidth()) / 2;
        int offsetY = tightMode ? 0 : 1;

        int total = items.size();

        boolean fog = !preview && Settings.isFogOfWarMode();
        Set<Integer> revealed = fog
                ? FogOfWar.revealedSlots(board.getWidth(), board.getHeight(),
                        progress.getSubmittedSlots(), Settings.isFogDiagonalReveal())
                : null;

        // In Lockout, cells claimed by another team show as plain barriers.
        Set<Integer> locked = preview ? Set.of() : Lockout.lockedSlots(viewer);

        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                int index = y * board.getWidth() + x;
                if (index >= items.size()) continue;

                int slot = (y + offsetY) * GUI_WIDTH + offsetX + x;
                inv.setItem(slot, cellIcon(board, progress, index, fog, revealed, locked, tagLoader, total, loc, preview));
            }
        }
    }

    /**
     * Renders an oversized board through the adaptive scrollable viewport: an
     * all-filler base, then the live scroll arrows and the recenter button at the
     * slots the {@link BingoViewport.Layout} chose, then the visible board cells.
     * Used by both the initial open and every live re-render, so a scroll always
     * lands in a fully consistent state.
     */
    private static void renderScroll(Inventory inv, BingoBoard board, BingoProgressAccess progress, Player viewer, SupportedLocale loc, boolean preview) {
        BingoViewport.Layout layout = BingoViewport.layout(viewer.getUniqueId(), board);

        fillBackground(inv);

        // Only the directions that can still scroll show an arrow; the rest stay
        // filler ("hidden"). The recenter button is always present.
        if (layout.upActive())    inv.setItem(layout.upSlot(),    arrow(loc, "board.scroll.up"));
        if (layout.downActive())  inv.setItem(layout.downSlot(),  arrow(loc, "board.scroll.down"));
        if (layout.leftActive())  inv.setItem(layout.leftSlot(),  arrow(loc, "board.scroll.left"));
        if (layout.rightActive()) inv.setItem(layout.rightSlot(), arrow(loc, "board.scroll.right"));
        if (layout.recenterSlot() >= 0) inv.setItem(layout.recenterSlot(), recenterButton(loc));

        BingoTagLoader tagLoader = ItemBingo.getInstance().getTagLoader();
        int total = board.getItems().size();
        boolean fog = !preview && Settings.isFogOfWarMode();
        Set<Integer> revealed = fog
                ? FogOfWar.revealedSlots(board.getWidth(), board.getHeight(),
                        progress.getSubmittedSlots(), Settings.isFogDiagonalReveal())
                : null;
        Set<Integer> locked = preview ? Set.of() : Lockout.lockedSlots(viewer);

        for (int index = 0; index < board.getItems().size(); index++) {
            int slot = BingoViewport.indexToSlot(index, board, layout);
            if (slot < 0) continue;
            inv.setItem(slot, cellIcon(board, progress, index, fog, revealed, locked, tagLoader, total, loc, preview));
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
            int total,
            SupportedLocale loc,
            boolean preview
    ) {
        // The preview shows the bare board — every cell is just its required item.
        if (preview) {
            return withTagLore(board.getItems().get(index), tagLoader, loc);
        }

        if (progress.isSubmitted(index)) {
            UUID owner = progress.getSubmitterId(index);
            return submittedIcon(board.getItems().get(index).getType(), owner, progress.getSubmitterName(index),
                    progressFraction(progress, owner, total), progress.getSubmissionTime(index), loc);
        } else if (locked.contains(index)) {
            return lockedIcon(loc);
        } else if (fog && !revealed.contains(index)) {
            return hiddenCell();
        } else {
            return withTagLore(board.getItems().get(index), tagLoader, loc);
        }
    }

    /** A scroll-arrow button (one per live edge); cosmetic only — handled by slot in the click listener. */
    private static ItemStack arrow(SupportedLocale loc, String nameKey) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Messages.get(loc, nameKey).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Messages.get(loc, "board.scroll.hint").decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    /** Jumps the viewport back to the middle of the board; handled by slot in the click listener. */
    private static ItemStack recenterButton(SupportedLocale loc) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Messages.get(loc, "board.recenter.name").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Messages.get(loc, "board.recenter.lore").decoration(TextDecoration.ITALIC, false)));
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
     * barrier with no durability bar. It just signals "another team got here
     * first — this cell is gone".
     */
    private static ItemStack lockedIcon(SupportedLocale loc) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Messages.get(loc, "board.locked.name").decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Messages.get(loc, "board.locked.lore1").decoration(TextDecoration.ITALIC, false));
        lore.add(Messages.get(loc, "board.locked.lore2").decoration(TextDecoration.ITALIC, false));
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

    /** Elapsed seconds as {@code H:MM:SS} (hours dropped when zero), for the submit-time lore. */
    private static String formatElapsed(long totalSeconds) {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return h > 0
                ? String.format("%d:%02d:%02d", h, m, s)
                : String.format("%02d:%02d", m, s);
    }

    /**
     * The icon shown for an already-submitted slot, used by both the initial
     * render and live updates so a submitted slot always looks the same.
     *
     * @param original      the cell's required item, shown by name in the lore
     * @param submitterId   submitter UUID, or {@code null} for the barrier variant
     * @param submitterName submitter name shown in the lore (team match only)
     * @param fraction      progress fraction in [0, 1] driving the durability bar
     * @param submitSeconds elapsed seconds when it was submitted, or -1 to omit
     * @param loc           the viewer's locale, for the displayed text
     */
    public static ItemStack submittedIcon(Material original, UUID submitterId, String submitterName,
                                          double fraction, long submitSeconds, SupportedLocale loc) {
        boolean teamMatch = submitterId != null;

        ItemStack item = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta meta = item.getItemMeta();

        meta.setItemModel(NamespacedKey.minecraft(teamMatch ? "player_head" : "barrier"));

        meta.displayName(Messages.get(loc, "board.submitted.name").decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Messages.get(loc, "board.submitted.lore").decoration(TextDecoration.ITALIC, false));
        // Original item — rendered translatable so it shows in the viewer's client
        // (or set) language, matching the "-> item" submission broadcast.
        lore.add(Messages.get(loc, "board.submitted.original")
                .append(Component.translatable(original.translationKey()).color(NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        if (teamMatch) {
            String name = submitterName != null ? submitterName : Messages.legacy(loc, "board.submitted.unknown");
            lore.add(Messages.get(loc, "board.submitted.by", "submitter", name)
                    .decoration(TextDecoration.ITALIC, false));
        }
        if (submitSeconds >= 0) {
            lore.add(Messages.get(loc, "board.submitted.time", "time", formatElapsed(submitSeconds))
                    .decoration(TextDecoration.ITALIC, false));
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

    private static ItemStack withTagLore(ItemStack original, BingoTagLoader tagLoader, SupportedLocale loc) {
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
            lore.add(tag.bullet(loc));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
