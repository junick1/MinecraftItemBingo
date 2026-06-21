package me.junick.itemBingo.admin;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.BundleManager;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.gui.DiamondExchangeGUI;
import me.junick.itemBingo.gui.EffectShopGUI;
import me.junick.itemBingo.gui.ItemShopGUI;
import me.junick.itemBingo.gui.MenuGUI;
import me.junick.itemBingo.gui.ShopGUI;
import me.junick.itemBingo.util.GuiSync;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.Set;

public class AdminClickListener implements Listener {
    private final ItemBingo plugin;

    public AdminClickListener(ItemBingo plugin) {
        this.plugin = plugin;
    }

    private boolean isAdminGuiTitle(String title) {
        return title.equals(AdminGUI.TITLE_TEAM)
                || title.equals(AdminGUI.TITLE_SHOP)
                || title.equals(AdminGUI.TITLE_VANILLA);
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

        // ===== 탭 이동 (0,1,2) =====
        if (slot == 0) {
            AdminGUI.openTeam(plugin, p);
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

        // ===== Team GUI =====
        if (title.equals(AdminGUI.TITLE_TEAM)) {
            if (slot == 13) {
                Settings.toggleTeamEnabled();
                refreshTeam();
                // Team mode flips every player between solo and team progress/
                // currency, so refresh all open boards and shops.
                GuiSync.refreshAllGameViews();
            }
            return;
        }

        // ===== Shop GUI =====
        if (title.equals(AdminGUI.TITLE_SHOP)) {
            if (slot == 11) {
                Settings.toggleShopEnabled();
                // Shop turned off entirely → kick anyone out of the shop menus.
                if (!Settings.isShopEnabled()) {
                    GuiSync.closeViewers(ShopGUI.TITLE);
                    GuiSync.closeViewers(EffectShopGUI.TITLE);
                    GuiSync.closeViewers(ItemShopGUI.TITLE);
                    GuiSync.closeViewers(DiamondExchangeGUI.TITLE);
                }
                // The main menu shows a shop enabled/disabled indicator — resync it.
                GuiSync.forEachViewer(MenuGUI.TITLE, vp -> MenuGUI.openMain(vp, false));
                refreshShop();
                return;
            }

            // 상점이 OFF면 하위 토글은 건드릴 수 없게
            if (!Settings.isShopEnabled()) return;

            if (slot == 13) {
                Settings.toggleEffectShopEnabled();
                if (!Settings.isEffectShopEnabled()) {
                    GuiSync.closeViewers(EffectShopGUI.TITLE);
                }
                refreshShop();
                return;
            }

            if (slot == 15) {
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
            switch(slot) {
                case 13 -> Settings.toggleShovelOxidizeCopper();
                case 14 -> Settings.togglePenaltyInt();
                case 15 -> Settings.togglePositionSwapMode();
                case 16 -> Settings.toggleSwapAlert();
                case 17 -> Settings.toggleSwapTimer();
            }
            if (13 <= slot && slot <= 17) {
                refreshVanilla();
            }
        }
    }

    // Re-render each admin tab for *every* admin currently viewing it, so two
    // people with the panel open stay in sync instead of seeing stale toggles.
    private void refreshTeam() {
        GuiSync.forEachViewer(AdminGUI.TITLE_TEAM, vp -> AdminGUI.openTeam(plugin, vp));
    }

    private void refreshShop() {
        GuiSync.forEachViewer(AdminGUI.TITLE_SHOP, vp -> AdminGUI.openShop(plugin, vp));
    }

    private void refreshVanilla() {
        GuiSync.forEachViewer(AdminGUI.TITLE_VANILLA, vp -> AdminGUI.openVanilla(plugin, vp));
    }
}
