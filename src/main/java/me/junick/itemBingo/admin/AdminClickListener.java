package me.junick.itemBingo.admin;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.gui.BingoGuiHolder;
import me.junick.itemBingo.gui.BingoGuiHolder.Gui;
import me.junick.itemBingo.gui.MenuGUI;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.network.ModSync;
import me.junick.itemBingo.util.BingoScoreboard;
import me.junick.itemBingo.util.ChestManager;
import me.junick.itemBingo.util.GuiSync;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

public class AdminClickListener implements Listener {
    private final ItemBingo plugin;

    public AdminClickListener(ItemBingo plugin) {
        this.plugin = plugin;
    }

    /** The admin screen {@code top} belongs to, or {@code null} if it isn't an admin GUI. */
    private static @Nullable Gui adminGui(Inventory top) {
        BingoGuiHolder h = BingoGuiHolder.of(top);
        if (h == null) return null;
        return switch (h.type()) {
            case ADMIN_GAME, ADMIN_SHOP, ADMIN_VANILLA, ADMIN_MODE -> h.type();
            default -> null;
        };
    }

    private boolean hasAdmin(Player p) {
        return p.isOp() && p.hasPermission("itembingo.admin");
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (adminGui(event.getView().getTopInventory()) != null) {
            Settings.save(ItemBingo.getInstance());
        }
    }

    @EventHandler
    public void onAdminClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        Gui screen = adminGui(e.getView().getTopInventory());
        if (screen == null) return;

        e.setCancelled(true);

        if (e.getRawSlot() >= e.getView().getTopInventory().getSize()) return;

        if (!hasAdmin(p)) {
            p.closeInventory();
            p.sendMessage(Messages.get(p, "admin.no-permission"));
            return;
        }

        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

        int slot = e.getRawSlot();

        // ===== Tab navigation (0,1,2,3) =====
        if (slot == 0) {
            AdminGUI.openGame(plugin, p);
            return;
        }
        if (slot == 1) {
            AdminGUI.openShop(plugin, p);
            return;
        }
        if (slot == 2) {
            AdminGUI.openVanilla(plugin, p);
            return;
        }
        if (slot == 3) {
            AdminGUI.openMode(plugin, p);
            return;
        }

        // ===== Game GUI =====
        if (screen == Gui.ADMIN_GAME) {
            switch (slot) {
                case 20 -> {
                    Settings.toggleTeamEnabled();
                    // Team mode flips solo/team progress + currency, the chest
                    // key/title, and whether /tpa is usable — resync everything.
                    ChestManager.invalidateOpenChests();
                    refreshGame();
                    refreshMenu();
                    GuiSync.refreshAllGameViews();
                }
                case 22 -> {
                    if (e.isRightClick()) {
                        Settings.cycleChestRowsDown();
                    } else {
                        Settings.cycleChestRowsUp();
                    }
                    // Capacity changed: rebuild open chests at the new size and
                    // refresh the menu's chest-button enabled indicator.
                    ChestManager.invalidateOpenChests();
                    refreshGame();
                    refreshMenu();
                }
                case 24 -> {
                    // /tpa toggle only applies when team mode is on.
                    if (Settings.isTeamEnabled()) {
                        Settings.toggleTpaEnabled();
                        refreshGame();
                    }
                }
                case 31 -> {
                    Settings.togglePenaltyInt();
                    refreshGame();
                }
                case 33 -> {
                    Settings.toggleHideLeaderboard();
                    refreshGame();
                    // Reflect the change on the sidebar immediately instead of
                    // waiting up to a second for the next scoreboard tick.
                    BingoScoreboard.updateAll();
                }
            }
            return;
        }

        // ===== Shop GUI =====
        if (screen == Gui.ADMIN_SHOP) {
            if (slot == 20) {
                Settings.toggleShopEnabled();
                // Shop turned off entirely → kick anyone out of the shop menus.
                if (!Settings.isShopEnabled()) {
                    GuiSync.closeViewers(Gui.SHOP);
                    GuiSync.closeViewers(Gui.EFFECT_SHOP);
                    GuiSync.closeViewers(Gui.ITEM_SHOP);
                    GuiSync.closeViewers(Gui.DIAMOND_EXCHANGE);
                }
                // The main menu shows a shop enabled/disabled indicator — resync it.
                refreshMenu();
                refreshShop();
                return;
            }

            // Sub-toggles are locked while the shop is OFF.
            if (!Settings.isShopEnabled()) return;

            if (slot == 22) {
                Settings.toggleEffectShopEnabled();
                if (!Settings.isEffectShopEnabled()) {
                    GuiSync.closeViewers(Gui.EFFECT_SHOP);
                }
                refreshShop();
                return;
            }

            if (slot == 24) {
                Settings.toggleItemShopEnabled();
                if (!Settings.isItemShopEnabled()) {
                    GuiSync.closeViewers(Gui.ITEM_SHOP);
                }
                refreshShop();
                return;
            }
            return;
        }

        // ===== Vanilla GUI =====
        if (screen == Gui.ADMIN_VANILLA) {
            if (slot == 22) {
                Settings.toggleShovelOxidizeCopper();
                refreshVanilla();
            }
            return;
        }

        // ===== Mode GUI =====
        if (screen == Gui.ADMIN_MODE) {
            // Mode switch (cycles Normal -> Swappage -> Fog of War -> Lockout).
            // Changing the mode changes both which sub-settings show and how the
            // board renders (fog hides/reveals cells), so re-render boards too.
            if (slot == 20) {
                Settings.cycleGameMode();
                refreshMode();
                GuiSync.refreshAllGameViews();
                return;
            }

            switch (Settings.getGameMode()) {
                case SWAPPAGE -> {
                    switch (slot) {
                        case 30 -> Settings.toggleSwapTimer();
                        case 32 -> Settings.toggleSwapAlert();
                        default -> { return; }
                    }
                    refreshMode();
                }
                case FOG_OF_WAR -> {
                    switch (slot) {
                        case 29 -> {
                            Settings.toggleFogSubmitLock();
                            // Doesn't change what the chest GUI shows, but modded
                            // clients display and enforce the flag — re-push it.
                            ModSync.broadcastBoard();
                        }
                        case 31 -> Settings.toggleFogRevealAlert();
                        case 33 -> {
                            // Diagonal reveal changes which cells are revealed, so
                            // refresh open boards.
                            Settings.toggleFogDiagonalReveal();
                            refreshMode();
                            GuiSync.refreshAllGameViews();
                            return;
                        }
                        default -> { return; }
                    }
                    refreshMode();
                }
                default -> { /* Normal / Lockout: no sub-settings */ }
            }
        }
    }

    // Re-render each admin tab for *every* admin currently viewing it, so two
    // people with the panel open stay in sync instead of seeing stale toggles.
    private void refreshGame() {
        GuiSync.forEachViewer(Gui.ADMIN_GAME, vp -> AdminGUI.openGame(plugin, vp));
    }

    private void refreshShop() {
        GuiSync.forEachViewer(Gui.ADMIN_SHOP, vp -> AdminGUI.openShop(plugin, vp));
    }

    private void refreshVanilla() {
        GuiSync.forEachViewer(Gui.ADMIN_VANILLA, vp -> AdminGUI.openVanilla(plugin, vp));
    }

    private void refreshMode() {
        GuiSync.forEachViewer(Gui.ADMIN_MODE, vp -> AdminGUI.openMode(plugin, vp));
    }

    private void refreshMenu() {
        GuiSync.forEachViewer(Gui.MENU, vp -> MenuGUI.openMain(vp, false));
    }
}
