package me.junick.itemBingo.admin;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

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
        Settings s = new Settings(plugin);

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
                s.toggleTeam();
                AdminGUI.openTeam(plugin, p);
            }
            return;
        }

        // ===== Shop GUI =====
        if (title.equals(AdminGUI.TITLE_SHOP)) {
            if (slot == 11) {
                s.toggleShop();
                AdminGUI.openShop(plugin, p);
                return;
            }

            // 상점이 OFF면 하위 토글은 건드릴 수 없게
            if (!s.isShopEnabled()) return;

            if (slot == 13) {
                s.toggleEffectShop();
                AdminGUI.openShop(plugin, p);
                return;
            }

            if (slot == 15) {
                s.toggleItemShop();
                AdminGUI.openShop(plugin, p);
                return;
            }
            return;
        }

        // ===== Vanilla GUI =====
        if (title.equals(AdminGUI.TITLE_VANILLA)) {
            if (slot == 13) {
                s.toggleShovelOxidizeCopper();
                AdminGUI.openVanilla(plugin, p);
            }
        }
    }
}
