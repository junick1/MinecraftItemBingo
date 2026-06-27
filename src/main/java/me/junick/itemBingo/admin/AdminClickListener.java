package me.junick.itemBingo.admin;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.gui.DiamondExchangeGUI;
import me.junick.itemBingo.gui.EffectShopGUI;
import me.junick.itemBingo.gui.ItemShopGUI;
import me.junick.itemBingo.gui.MenuGUI;
import me.junick.itemBingo.gui.ShopGUI;
import me.junick.itemBingo.util.BingoScoreboard;
import me.junick.itemBingo.util.ChestManager;
import me.junick.itemBingo.util.GuiSync;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class AdminClickListener implements Listener {
    private final ItemBingo plugin;

    public AdminClickListener(ItemBingo plugin) {
        this.plugin = plugin;
    }

    private boolean isAdminGuiTitle(String title) {
        return title.equals(AdminGUI.TITLE_GAME)
                || title.equals(AdminGUI.TITLE_SHOP)
                || title.equals(AdminGUI.TITLE_VANILLA)
                || title.equals(AdminGUI.TITLE_MODE);
    }

    private boolean hasAdmin(Player p) {
        return p.isOp() && p.hasPermission("itembingo.admin");
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (isAdminGuiTitle(event.getView().getTitle())) {
            Settings.save(ItemBingo.getInstance());
        }
    }

    @EventHandler
    public void onAdminClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        String title = e.getView().getTitle();
        if (!isAdminGuiTitle(title)) return;

        e.setCancelled(true);

        if (e.getRawSlot() >= e.getView().getTopInventory().getSize()) return;

        if (!hasAdmin(p)) {
            p.closeInventory();
            p.sendMessage(Component.text("No permissions.", NamedTextColor.RED));
            return;
        }

        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

        int slot = e.getRawSlot();

        // ===== 탭 이동 (0,1,2,3) =====
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
        if (title.equals(AdminGUI.TITLE_GAME)) {
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
        if (title.equals(AdminGUI.TITLE_SHOP)) {
            if (slot == 20) {
                Settings.toggleShopEnabled();
                // Shop turned off entirely → kick anyone out of the shop menus.
                if (!Settings.isShopEnabled()) {
                    GuiSync.closeViewers(ShopGUI.TITLE);
                    GuiSync.closeViewers(EffectShopGUI.TITLE);
                    GuiSync.closeViewers(ItemShopGUI.TITLE);
                    GuiSync.closeViewers(DiamondExchangeGUI.TITLE);
                }
                // The main menu shows a shop enabled/disabled indicator — resync it.
                refreshMenu();
                refreshShop();
                return;
            }

            // 상점이 OFF면 하위 토글은 건드릴 수 없게
            if (!Settings.isShopEnabled()) return;

            if (slot == 22) {
                Settings.toggleEffectShopEnabled();
                if (!Settings.isEffectShopEnabled()) {
                    GuiSync.closeViewers(EffectShopGUI.TITLE);
                }
                refreshShop();
                return;
            }

            if (slot == 24) {
                Settings.toggleItemShopEnabled();
                if (!Settings.isItemShopEnabled()) {
                    GuiSync.closeViewers(ItemShopGUI.TITLE);
                }
                refreshShop();
                return;
            }
            return;
        }

        // ===== Vanilla GUI =====
        if (title.equals(AdminGUI.TITLE_VANILLA)) {
            if (slot == 22) {
                Settings.toggleShovelOxidizeCopper();
                refreshVanilla();
            }
            return;
        }

        // ===== Mode GUI =====
        if (title.equals(AdminGUI.TITLE_MODE)) {
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
                        case 29 -> Settings.toggleFogSubmitLock();
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
        GuiSync.forEachViewer(AdminGUI.TITLE_GAME, vp -> AdminGUI.openGame(plugin, vp));
    }

    private void refreshShop() {
        GuiSync.forEachViewer(AdminGUI.TITLE_SHOP, vp -> AdminGUI.openShop(plugin, vp));
    }

    private void refreshVanilla() {
        GuiSync.forEachViewer(AdminGUI.TITLE_VANILLA, vp -> AdminGUI.openVanilla(plugin, vp));
    }

    private void refreshMode() {
        GuiSync.forEachViewer(AdminGUI.TITLE_MODE, vp -> AdminGUI.openMode(plugin, vp));
    }

    private void refreshMenu() {
        GuiSync.forEachViewer(MenuGUI.TITLE, vp -> MenuGUI.openMain(vp, false));
    }
}
