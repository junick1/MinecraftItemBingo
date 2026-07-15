package me.junick.itemBingo.util;

import me.junick.itemBingo.gui.BingoGUI;
import me.junick.itemBingo.gui.BingoGuiHolder;
import me.junick.itemBingo.gui.DiamondExchangeGUI;
import me.junick.itemBingo.gui.EffectShopGUI;
import me.junick.itemBingo.gui.ItemShopGUI;
import me.junick.itemBingo.gui.MenuGUI;
import me.junick.itemBingo.gui.ShopGUI;
import me.junick.itemBingo.gui.SummaryGUI;
import me.junick.itemBingo.network.ModSync;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Helpers for keeping shared inventory GUIs consistent across every player who
 * currently has them open. GUIs are matched by their (legacy section) title,
 * which is how the rest of the plugin identifies them.
 */
public final class GuiSync {
    private GuiSync() {}

    /**
     * Runs {@code action} for every online player whose currently open inventory
     * has the given title.
     * <p>
     * Viewers are snapshotted before the action runs, because the action may
     * reopen or close inventories (which would otherwise mutate the views while
     * we iterate).
     */
    public static void forEachViewer(String title, Consumer<Player> action) {
        List<Player> viewers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (title.equals(p.getOpenInventory().getTitle())) {
                viewers.add(p);
            }
        }
        for (Player p : viewers) {
            action.accept(p);
        }
    }

    /** Closes the inventory of every player currently viewing the given title. */
    public static void closeViewers(String title) {
        forEachViewer(title, Player::closeInventory);
    }

    /**
     * Holder-based viewer iteration: runs {@code action} for every online player
     * whose open GUI is one of ours of the given {@code type}. Preferred over the
     * title-based overload, since GUI titles are now localized per player.
     */
    public static void forEachViewer(BingoGuiHolder.Gui type, Consumer<Player> action) {
        List<Player> viewers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (BingoGuiHolder.is(p.getOpenInventory().getTopInventory(), type)) {
                viewers.add(p);
            }
        }
        for (Player p : viewers) {
            action.accept(p);
        }
    }

    /** Holder-based: closes the inventory of every viewer of a GUI of {@code type}. */
    public static void closeViewers(BingoGuiHolder.Gui type) {
        forEachViewer(type, Player::closeInventory);
    }

    /**
     * Re-renders the currency-displaying shop GUIs (item / effect shop) for each
     * given player who currently has one open, so a shared-currency change made
     * by one teammate updates everyone else's open shop.
     * <p>
     * Players who don't have a shop open are skipped — this never force-opens a
     * shop on someone. Pass {@code progress.viewers(actor)} to target exactly the
     * players whose balance was affected (the actor alone when solo, the whole
     * team when in team mode).
     */
    public static void refreshShops(Iterable<? extends Player> players) {
        for (Player p : players) {
            Inventory top = p.getOpenInventory().getTopInventory();
            if (BingoGuiHolder.is(top, BingoGuiHolder.Gui.ITEM_SHOP)) {
                ItemShopGUI.open(p);
            } else if (BingoGuiHolder.is(top, BingoGuiHolder.Gui.EFFECT_SHOP)) {
                EffectShopGUI.open(p);
            }
        }
    }

    /**
     * Re-renders the board and both shops for everyone currently viewing them.
     * Used when the underlying dataset changes for every player at once — a new
     * board roll or a team-mode toggle, both of which swap which progress and
     * currency each player should see.
     */
    public static void refreshAllGameViews() {
        // A team-mode change can revoke someone's right to see the board (e.g. an
        // unassigned player once team mode turns on), so close their board instead
        // of reopening it.
        forEachViewer(BingoGuiHolder.Gui.BINGO, vp -> {
            if (BingoGUI.canView(vp)) {
                BingoGUI.open(vp);
            } else {
                vp.closeInventory();
            }
        });
        forEachViewer(BingoGuiHolder.Gui.ITEM_SHOP, ItemShopGUI::open);
        forEachViewer(BingoGuiHolder.Gui.EFFECT_SHOP, EffectShopGUI::open);

        // Companion-mod clients keep board state client-side (screen + HUD), so
        // any bulk change that reopens GUIs must also re-push their view.
        ModSync.broadcastBoard();
    }

    /**
     * Reopens {@code p}'s currently open plugin GUI in their (possibly just
     * changed) language. Used by {@code /language} so a switch is reflected
     * immediately. GUIs are recognized by their {@link BingoGuiHolder} marker, so
     * a player who has no plugin GUI open — or has an admin/editor screen open
     * that we don't live-refresh — is left untouched.
     */
    public static void reopenFor(Player p) {
        BingoGuiHolder holder = BingoGuiHolder.of(p.getOpenInventory().getTopInventory());
        if (holder == null) return;

        switch (holder.type()) {
            case BINGO -> {
                if (BingoGUI.canView(p)) BingoGUI.open(p);
                else p.closeInventory();
            }
            case SHOP -> ShopGUI.open(p);
            case ITEM_SHOP -> ItemShopGUI.open(p);
            case EFFECT_SHOP -> EffectShopGUI.open(p);
            case MENU -> MenuGUI.openMain(p, false);
            case SUMMARY -> SummaryGUI.tryOpen(p);
            case DIAMOND_EXCHANGE -> {
                int amount = 1;
                if (holder.context() != null) {
                    try {
                        amount = Integer.parseInt(holder.context());
                    } catch (NumberFormatException ignored) {
                        // fall back to 1
                    }
                }
                DiamondExchangeGUI.open(p, amount);
            }
            default -> {
                // BUNDLE / PRESET / PRESET_EDITOR / MAP_SELECTOR / admin screens:
                // not live-reopened; they refresh on the next manual open.
            }
        }
    }
}
