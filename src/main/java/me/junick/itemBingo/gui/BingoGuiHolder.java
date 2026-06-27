package me.junick.itemBingo.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Identity marker for the plugin's GUIs. Each GUI is created with one of these as
 * its {@link InventoryHolder}, so click handlers and {@link me.junick.itemBingo.util.GuiSync}
 * recognize a GUI by its {@link Gui} type rather than by its (now localized,
 * per-player) title.
 *
 * <p>An optional {@link #context() context} string carries GUI-specific state that
 * used to be parsed back out of the title — e.g. the preset id for the preset
 * editor.</p>
 */
public final class BingoGuiHolder implements InventoryHolder {

    public enum Gui {
        BINGO, SHOP, ITEM_SHOP, EFFECT_SHOP, DIAMOND_EXCHANGE,
        MENU, BUNDLE, PRESET, PRESET_EDITOR, SUMMARY, MAP_SELECTOR, DYE_SELECTOR,
        ADMIN_GAME, ADMIN_SHOP, ADMIN_VANILLA, ADMIN_MODE
    }

    private final Gui type;
    private final String context;
    private Inventory inventory;

    public BingoGuiHolder(Gui type) {
        this(type, null);
    }

    public BingoGuiHolder(Gui type, @Nullable String context) {
        this.type = type;
        this.context = context;
    }

    public Gui type() {
        return type;
    }

    public @Nullable String context() {
        return context;
    }

    /** Builders call this right after {@code Bukkit.createInventory(holder, …)}. */
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    /* ===================== Identity helpers ===================== */

    /** The holder of {@code inv} if it is one of our GUIs, else {@code null}. */
    public static @Nullable BingoGuiHolder of(@Nullable Inventory inv) {
        return (inv != null && inv.getHolder() instanceof BingoGuiHolder h) ? h : null;
    }

    /** Whether {@code inv} is one of our GUIs of the given {@code type}. */
    public static boolean is(@Nullable Inventory inv, Gui type) {
        BingoGuiHolder h = of(inv);
        return h != null && h.type == type;
    }
}
