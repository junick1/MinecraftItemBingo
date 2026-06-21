package me.junick.itemBingo.util;

import me.junick.itemBingo.gui.BingoGUI;
import me.junick.itemBingo.gui.EffectShopGUI;
import me.junick.itemBingo.gui.ItemShopGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
            String title = p.getOpenInventory().getTitle();
            if (ItemShopGUI.TITLE.equals(title)) {
                ItemShopGUI.open(p);
            } else if (EffectShopGUI.TITLE.equals(title)) {
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
        forEachViewer(BingoGUI.TITLE, vp -> {
            if (BingoGUI.canView(vp)) {
                BingoGUI.open(vp);
            } else {
                vp.closeInventory();
            }
        });
        forEachViewer(ItemShopGUI.TITLE, ItemShopGUI::open);
        forEachViewer(EffectShopGUI.TITLE, EffectShopGUI::open);
    }
}
