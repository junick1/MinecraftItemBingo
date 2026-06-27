package me.junick.itemBingo.gui;

import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import me.junick.itemBingo.util.IconGenerator;
import me.junick.itemBingo.util.TimerManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MenuGUI {
    /** Title message key. GUI identity is the {@link BingoGuiHolder} marker, not the title. */
    public static final String TITLE_KEY = "gui.menu.title";

    /** Button slots (used by {@code MenuClickEvent} for locale-independent routing). */
    public static final int SLOT_RANK = 10;
    public static final int SLOT_BOARD = 12;
    public static final int SLOT_SHOP = 14;
    public static final int SLOT_CHEST = 16;
    public static final int SLOT_RESULTS = 22;

    public static void openMain(Player p) {
        openMain(p, true);
    }

    /**
     * @param playSound false when re-rendering an already-open menu (e.g. a live
     *                  refresh), so the open sound isn't replayed on every update.
     */
    public static void openMain(Player p, boolean playSound) {
        SupportedLocale loc = Messages.localeOf(p);

        BingoGuiHolder holder = new BingoGuiHolder(BingoGuiHolder.Gui.MENU);
        Inventory inv = Bukkit.createInventory(holder, 27, Messages.get(loc, TITLE_KEY));
        holder.setInventory(inv);

        ItemStack grayGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta grayMeta = grayGlass.getItemMeta(); grayMeta.setHideTooltip(true); grayGlass.setItemMeta(grayMeta);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, grayGlass);
        }

        inv.setItem(SLOT_RANK, IconGenerator.icon(Material.OAK_SIGN,
                Messages.legacy(loc, "gui.menu.rank.name"), Messages.legacy(loc, "gui.menu.rank.lore")));
        inv.setItem(SLOT_BOARD, IconGenerator.icon(Material.MAP,
                Messages.legacy(loc, "gui.menu.board.name"), Messages.legacy(loc, "gui.menu.board.lore")));
        inv.setItem(SLOT_SHOP, Settings.isShopEnabled()
                ? IconGenerator.icon(Material.EMERALD, Messages.legacy(loc, "gui.menu.shop.name"), Messages.legacy(loc, "gui.menu.shop.lore"))
                : IconGenerator.icon(Material.GRAY_DYE, Messages.legacy(loc, "gui.menu.shop-disabled.name"), Messages.legacy(loc, "common.disabled-by-admin")));
        inv.setItem(SLOT_CHEST, Settings.isChestEnabled()
                ? IconGenerator.icon(Material.ENDER_CHEST, Messages.legacy(loc, "gui.menu.chest.name"),
                        Settings.isTeamEnabled() ? Messages.legacy(loc, "gui.menu.chest.lore-team") : Messages.legacy(loc, "gui.menu.chest.lore-solo"))
                : IconGenerator.icon(Material.GRAY_DYE, Messages.legacy(loc, "gui.menu.chest-disabled.name"), Messages.legacy(loc, "common.disabled-by-admin")));

        // Last game's results — only available once the timer stops.
        inv.setItem(SLOT_RESULTS, TimerManager.isRunning()
                ? IconGenerator.icon(Material.GRAY_DYE, Messages.legacy(loc, "gui.menu.results-running.name"), Messages.legacy(loc, "gui.menu.results-running.lore"))
                : IconGenerator.icon(Material.KNOWLEDGE_BOOK, Messages.legacy(loc, "gui.menu.results.name"), Messages.legacy(loc, "gui.menu.results.lore")));

        if (playSound) {
            p.playSound(p.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
        }
        p.openInventory(inv);
    }
}
