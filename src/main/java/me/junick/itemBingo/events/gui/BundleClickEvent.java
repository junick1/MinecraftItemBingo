package me.junick.itemBingo.events.gui;

import me.junick.itemBingo.config.BundleManager;
import me.junick.itemBingo.gui.BingoGuiHolder;
import me.junick.itemBingo.gui.BundleGUI;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class BundleClickEvent implements Listener {

    private static boolean isBundleGui(Inventory top) {
        return BingoGuiHolder.is(top, BingoGuiHolder.Gui.BUNDLE);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Inventory top = e.getView().getTopInventory();
        if (!isBundleGui(top)) return;

        int templateIdx = BundleGUI.readTemplateIndex(top);

        int raw = e.getRawSlot();
        boolean clickedTop = raw < top.getSize();

        // Template selector row (terracotta) — switch templates.
        if (clickedTop && raw >= BundleGUI.TEMPLATE_ROW_START) {
            e.setCancelled(true);
            int target = raw - BundleGUI.TEMPLATE_ROW_START;
            if (target == templateIdx) return;

            BundleGUI.persistEditable(top, templateIdx); // save edits before leaving
            BundleManager.setSelectedTemplate(target);
            BundleGUI.open(p, target);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            return;
        }

        // Decorative zones (top/separator rows) are never editable.
        if (clickedTop && (raw < BundleGUI.EDIT_START || raw > BundleGUI.EDIT_END)) {
            e.setCancelled(true);
            return;
        }

        // Template 0 is read-only: block anything that could place items into it.
        if (templateIdx == 0) {
            if (clickedTop || e.isShiftClick()) {
                e.setCancelled(true);
            }
            // otherwise: let the admin rearrange their own inventory freely
        }
        // template 1..8: editable region (9-35) and the player's own inventory are unrestricted
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Inventory top = e.getView().getTopInventory();
        if (!isBundleGui(top)) return;

        int templateIdx = BundleGUI.readTemplateIndex(top);
        int topSize = top.getSize();

        // Block the whole drag if it touches any non-editable top slot: read-only template 0, or
        // anything outside the editable region (so items can't land on the decorations/selectors).
        for (int raw : e.getRawSlots()) {
            if (raw < topSize && (templateIdx == 0 || raw < BundleGUI.EDIT_START || raw > BundleGUI.EDIT_END)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!isBundleGui(top)) return;

        BundleGUI.persistEditable(top, BundleGUI.readTemplateIndex(top));
    }
}
