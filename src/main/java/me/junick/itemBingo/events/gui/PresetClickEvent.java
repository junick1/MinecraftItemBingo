package me.junick.itemBingo.events.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.PresetManager;
import me.junick.itemBingo.config.PresetManager.PresetInfo;
import me.junick.itemBingo.gui.BingoGuiHolder;
import me.junick.itemBingo.gui.PresetEditorGUI;
import me.junick.itemBingo.gui.PresetGUI;
import me.junick.itemBingo.gui.PresetGUI.Mode;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.model.BingoBoard;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Handles clicks in the {@link PresetGUI} browser (both apply and edit modes). The whole GUI is
 * read-only: every click is cancelled, nav buttons page through the catalog, and a preset icon
 * either applies that board (apply mode) or opens it in the editor (edit mode).
 */
public class PresetClickEvent implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        BingoGuiHolder holder = BingoGuiHolder.of(e.getView().getTopInventory());
        if (holder == null || holder.type() != BingoGuiHolder.Gui.PRESET) return;
        Mode mode = Mode.valueOf(holder.context());

        e.setCancelled(true); // read-only browser — nothing is ever moved

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();

        Integer navPage = meta.getPersistentDataContainer().get(PresetGUI.navKey(), PersistentDataType.INTEGER);
        if (navPage != null) {
            PresetGUI.open(p, navPage, mode);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            return;
        }

        String id = meta.getPersistentDataContainer().get(PresetGUI.presetKey(), PersistentDataType.STRING);
        if (id == null) return;

        if (mode == Mode.EDIT) {
            PresetEditorGUI.open(p, id);
            return;
        }

        // Apply mode.
        PresetInfo info = PresetManager.getInfo(id);
        if (info == null) {
            p.sendMessage(Messages.get(p, "command.setbingo.not-found", "id", id));
            return;
        }
        if (!info.isComplete()) {
            p.sendMessage(Messages.get(p, "command.setbingo.incomplete",
                    "filled", info.filled(), "slots", info.slotCount(), "id", id));
            return;
        }

        BingoBoard board = PresetManager.load(id);
        ItemBingo.applyNewBoard(board);
        p.closeInventory();
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        p.sendMessage(Messages.get(p, "command.setbingo.applied",
                "id", id, "width", board.getWidth(), "height", board.getHeight()));
    }
}
