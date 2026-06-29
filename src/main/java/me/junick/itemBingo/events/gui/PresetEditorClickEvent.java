package me.junick.itemBingo.events.gui;

import me.junick.itemBingo.config.PresetManager;
import me.junick.itemBingo.config.PresetManager.PresetInfo;
import me.junick.itemBingo.gui.BingoGuiHolder;
import me.junick.itemBingo.gui.PresetEditorGUI;
import me.junick.itemBingo.util.BingoItemSelector;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Drives the {@link PresetEditorGUI}: board cells are freely editable, the control buttons run their
 * action, and the border panes are inert. The layout is saved on close (and after a reroll). The
 * preset id is read from the {@link BingoGuiHolder} context.
 */
public class PresetEditorClickEvent implements Listener {

    private static @Nullable String presetId(Inventory top) {
        BingoGuiHolder h = BingoGuiHolder.of(top);
        return (h != null && h.type() == BingoGuiHolder.Gui.PRESET_EDITOR) ? h.context() : null;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Inventory top = e.getView().getTopInventory();
        String id = presetId(top);
        if (id == null) return;

        PresetInfo info = PresetManager.getInfo(id);
        if (info == null) { // preset deleted out from under the editor
            e.setCancelled(true);
            p.closeInventory();
            return;
        }
        int width = info.width(), height = info.height();

        int raw = e.getRawSlot();
        boolean clickedTop = raw < top.getSize();

        // Control buttons.
        String action = buttonAction(e.getCurrentItem());
        if (clickedTop && action != null) {
            e.setCancelled(true);
            if (action.equals("reroll")) {
                List<ItemStack> items = BingoItemSelector.getWeightedRandomSurvivalItems(width * height);
                int i = 0;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        top.setItem(y * 9 + x, items.get(i++));
                    }
                }
                PresetEditorGUI.persist(top, id, width, height);
                p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
            } else if (action.equals("close")) {
                p.closeInventory();
            }
            return;
        }

        // Top inventory: board cells are editable, everything else (border panes) is locked.
        if (clickedTop && !PresetEditorGUI.isBoardCell(raw, width, height)) {
            e.setCancelled(true);
        }
        // Board cells and the player's own inventory are left unrestricted (drag/shift-click in).
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Inventory top = e.getView().getTopInventory();
        String id = presetId(top);
        if (id == null) return;

        PresetInfo info = PresetManager.getInfo(id);
        if (info == null) {
            e.setCancelled(true);
            return;
        }

        // Cancel the whole drag if any affected top-inventory slot is outside the board grid,
        // so dragged items can never land on (and be lost to) the border or control slots.
        int topSize = top.getSize();
        for (int raw : e.getRawSlots()) {
            if (raw < topSize && !PresetEditorGUI.isBoardCell(raw, info.width(), info.height())) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Inventory top = e.getView().getTopInventory();
        String id = presetId(top);
        if (id == null) return;

        PresetInfo info = PresetManager.getInfo(id);
        if (info == null) return;
        PresetEditorGUI.persist(top, id, info.width(), info.height());
    }

    private static String buttonAction(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(PresetEditorGUI.buttonKey(), PersistentDataType.STRING);
    }
}
