package me.junick.itemBingo.admin;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.gui.BingoGuiHolder;
import me.junick.itemBingo.gui.BingoGuiHolder.Gui;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * The admin configuration panel. Four tabs (Game / Shop / Vanilla / Mode), each a
 * 54-slot inventory identified by a {@link BingoGuiHolder} marker (not its title,
 * which is now localized). Toggle/label text is resolved per the opening admin's
 * locale.
 */
public class AdminGUI {

    /** Every admin tab is the same size for a consistent, polished look. */
    public static final int SIZE = 54;

    public static void openMain(ItemBingo plugin, Player p) {
        openGame(plugin, p);
    }

    public static void openGame(ItemBingo plugin, Player p) {
        SupportedLocale loc = Messages.localeOf(p);
        Inventory inv = create(p, Gui.ADMIN_GAME, "admin.game.title", loc);

        // Team mode on/off.
        inv.setItem(20, toggleItem(loc, Material.REDSTONE_TORCH,
                Messages.legacy(loc, "admin.game.team"), Settings.isTeamEnabled(),
                descs(loc, "admin.game.team-desc")));

        // Shared/private storage chest capacity.
        inv.setItem(22, chestCapacityItem(loc));

        // /tpa — only meaningful when team mode is on.
        inv.setItem(24, tpaItem(loc));

        // Submissions only while a game is running.
        inv.setItem(29, toggleItem(loc, Material.CLOCK,
                Messages.legacy(loc, "admin.game.require-timer"), Settings.isRequireTimerForSubmits(),
                descs(loc, "admin.game.require-timer-desc1", "admin.game.require-timer-desc2")));

        // Score calculation (penalty).
        inv.setItem(31, toggleChoiceItem(loc, Material.COMPARATOR,
                Messages.legacy(loc, "admin.game.penalty"), Settings.getPenaltyDisplay(loc),
                Settings.getPenaltyInt() + 1,
                descs(loc, "admin.game.penalty-desc1", "admin.game.penalty-desc2")));

        // Hide the ranking sidebar + /rank during the game.
        inv.setItem(33, toggleItem(loc, Material.BOOK,
                Messages.legacy(loc, "admin.game.hide-rank"), Settings.isHideLeaderboard(),
                descs(loc, "admin.game.hide-rank-desc1", "admin.game.hide-rank-desc2")));

        p.openInventory(inv);
    }

    public static void openShop(ItemBingo plugin, Player p) {
        SupportedLocale loc = Messages.localeOf(p);
        Inventory inv = create(p, Gui.ADMIN_SHOP, "admin.shop.title", loc);

        boolean shopOn = Settings.isShopEnabled();

        inv.setItem(20, toggleItem(loc, Material.EMERALD,
                Messages.legacy(loc, "admin.shop.shop"), shopOn, descs(loc, "admin.shop.shop-desc")));

        inv.setItem(22, shopOn
                ? toggleItem(loc, Material.POTION, Messages.legacy(loc, "admin.shop.effect"), Settings.isEffectShopEnabled(), descs(loc, "admin.shop.effect-desc"))
                : disabledItem(loc, Material.GRAY_DYE, Messages.legacy(loc, "admin.shop.effect"), descs(loc, "admin.shop.locked-reason")));

        inv.setItem(24, shopOn
                ? toggleItem(loc, Material.CHEST, Messages.legacy(loc, "admin.shop.item"), Settings.isItemShopEnabled(), descs(loc, "admin.shop.item-desc"))
                : disabledItem(loc, Material.GRAY_DYE, Messages.legacy(loc, "admin.shop.item"), descs(loc, "admin.shop.locked-reason")));

        p.openInventory(inv);
    }

    public static void openVanilla(ItemBingo plugin, Player p) {
        SupportedLocale loc = Messages.localeOf(p);
        Inventory inv = create(p, Gui.ADMIN_VANILLA, "admin.vanilla.title", loc);

        inv.setItem(22, toggleItem(loc, Material.IRON_SHOVEL,
                Messages.legacy(loc, "admin.vanilla.shovel-copper"), Settings.isShovelOxidizeCopper(),
                descs(loc, "admin.vanilla.shovel-copper-desc1", "admin.vanilla.shovel-copper-desc2")));

        inv.setItem(31, placeholder(loc, "admin.vanilla.placeholder", "admin.vanilla.placeholder-desc"));

        p.openInventory(inv);
    }

    public static void openMode(ItemBingo plugin, Player p) {
        SupportedLocale loc = Messages.localeOf(p);
        Inventory inv = create(p, Gui.ADMIN_MODE, "admin.mode.title", loc);

        // The mode switch button.
        inv.setItem(20, modeSwitchItem(loc));

        // Settings specific to the currently selected mode.
        switch (Settings.getGameMode()) {
            case NORMAL -> inv.setItem(31, placeholder(loc, "admin.mode.none-name", "admin.mode.none-normal-desc"));

            case LOCKOUT -> inv.setItem(31, placeholder(loc, "admin.mode.none-name", "admin.mode.none-lockout-desc"));

            case SWAPPAGE -> {
                inv.setItem(30, toggleItem(loc, Material.CLOCK,
                        Messages.legacy(loc, "admin.mode.swap-timer"), Settings.isSwapTimer(),
                        descs(loc, "admin.mode.swap-timer-desc1", "admin.default-off")));
                inv.setItem(32, toggleItem(loc, Material.ENDER_PEARL,
                        Messages.legacy(loc, "admin.mode.swap-alert"), Settings.isSwapAlert(),
                        descs(loc, "admin.mode.swap-alert-desc1", "admin.default-off")));
            }

            case FOG_OF_WAR -> {
                inv.setItem(29, toggleItem(loc, Material.BARRIER,
                        Messages.legacy(loc, "admin.mode.fog-submit"), Settings.isFogSubmitLock(),
                        descs(loc, "admin.mode.fog-submit-desc1", "admin.default-off")));
                inv.setItem(31, toggleItem(loc, Material.AMETHYST_SHARD,
                        Messages.legacy(loc, "admin.mode.fog-reveal"), Settings.isFogRevealAlert(),
                        descs(loc, "admin.mode.fog-reveal-desc1", "admin.default-on")));
                inv.setItem(33, toggleItem(loc, Material.RECOVERY_COMPASS,
                        Messages.legacy(loc, "admin.mode.fog-diagonal"), Settings.isFogDiagonalReveal(),
                        descs(loc, "admin.mode.fog-diagonal-desc1", "admin.default-off")));
            }
        }

        p.openInventory(inv);
    }

    /* ========================= builders ========================= */

    private static Inventory create(Player p, Gui type, String titleKey, SupportedLocale loc) {
        BingoGuiHolder holder = new BingoGuiHolder(type);
        Inventory inv = Bukkit.createInventory(holder, SIZE, Messages.get(loc, titleKey));
        holder.setInventory(inv);
        decorate(inv, type, loc);
        return inv;
    }

    /** Builds a list of gray lore lines from message keys (values carry their own §7). */
    private static List<Component> descs(SupportedLocale loc, String... keys) {
        List<Component> out = new ArrayList<>(keys.length);
        for (String k : keys) out.add(Messages.get(loc, k));
        return out;
    }

    private static ItemStack chestCapacityItem(SupportedLocale loc) {
        int rows = Settings.getChestRows();
        String label = Messages.legacy(loc, Settings.isTeamEnabled() ? "admin.game.chest-team" : "admin.game.chest-solo");
        String value = rows == 0
                ? Messages.legacy(loc, "admin.game.chest-disabled")
                : Messages.legacy(loc, "admin.game.chest-rows", "rows", rows);

        ItemStack it = new ItemStack(Material.ENDER_CHEST);
        it.setAmount(Math.max(1, rows));
        ItemMeta meta = it.getItemMeta();
        meta.displayName((Component.text(label + " : ", NamedTextColor.AQUA)
                .append(Component.text(value, rows == 0 ? NamedTextColor.RED : NamedTextColor.GREEN)))
                .decoration(TextDecoration.ITALIC, false));

        var lore = new ArrayList<Component>();
        lore.add(Messages.get(loc, Settings.isTeamEnabled() ? "admin.game.chest-desc-team" : "admin.game.chest-desc-solo")
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Messages.get(loc, "admin.game.chest-range").decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Messages.get(loc, "admin.game.chest-up").decoration(TextDecoration.ITALIC, false));
        lore.add(Messages.get(loc, "admin.game.chest-down").decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack tpaItem(SupportedLocale loc) {
        ItemStack it = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = it.getItemMeta();

        if (!Settings.isTeamEnabled()) {
            meta.displayName((Component.text("/tpa : ", NamedTextColor.AQUA)
                    .append(Component.text("DISABLED", NamedTextColor.DARK_GRAY)))
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Messages.get(loc, "admin.game.tpa-desc").decoration(TextDecoration.ITALIC, false),
                    Messages.get(loc, "admin.game.tpa-needs-team").decoration(TextDecoration.ITALIC, false)
            ));
            it.setItemMeta(meta);
            return it;
        }

        boolean on = Settings.isTpaEnabled();
        meta.displayName((Component.text("/tpa : ", NamedTextColor.AQUA)
                .append(Component.text(on ? "ON" : "OFF", on ? NamedTextColor.GREEN : NamedTextColor.RED)))
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Messages.get(loc, "admin.game.tpa-desc").decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Messages.get(loc, "admin.click-to-toggle").decoration(TextDecoration.ITALIC, false)
        ));
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack modeSwitchItem(SupportedLocale loc) {
        Settings.GameMode mode = Settings.getGameMode();
        Material mat = switch (mode) {
            case NORMAL -> Material.WHITE_WOOL;
            case SWAPPAGE -> Material.ENDER_EYE;
            case FOG_OF_WAR -> Material.LIGHT_GRAY_STAINED_GLASS;
            case LOCKOUT -> Material.IRON_BARS;
        };

        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName((Component.text(Messages.legacy(loc, "admin.mode.label") + " : ", NamedTextColor.AQUA)
                .append(Component.text(mode.displayName(loc), NamedTextColor.GREEN)))
                .decoration(TextDecoration.ITALIC, false));

        var lore = new ArrayList<Component>();
        for (Settings.GameMode m : Settings.GameMode.values()) {
            boolean cur = (m == mode);
            lore.add(Component.text((cur ? "▶ " : "   ") + m.displayName(loc),
                    cur ? NamedTextColor.GOLD : NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Messages.get(loc, "admin.mode.click-cycle").decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        it.setItemMeta(meta);
        return it;
    }

    /** Fills the inventory with the background frame and lays out the tab row. */
    private static void decorate(Inventory inv, Gui selected, SupportedLocale loc) {
        ItemStack f = filler();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, f);
        }

        inv.setItem(0, tabItem(loc, Material.WHITE_BANNER, "admin.tab.game", selected == Gui.ADMIN_GAME));
        inv.setItem(1, tabItem(loc, Material.GREEN_BANNER, "admin.tab.shop", selected == Gui.ADMIN_SHOP));
        inv.setItem(2, tabItem(loc, Material.ORANGE_BANNER, "admin.tab.vanilla", selected == Gui.ADMIN_VANILLA));
        inv.setItem(3, tabItem(loc, Material.PURPLE_BANNER, "admin.tab.mode", selected == Gui.ADMIN_MODE));
    }

    private static ItemStack filler() {
        ItemStack it = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = it.getItemMeta();
        meta.setHideTooltip(true);
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack tabItem(SupportedLocale loc, Material mat, String nameKey, boolean selected) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(
                Component.text((selected ? "▶ " : "") + Messages.legacy(loc, nameKey), selected ? NamedTextColor.GOLD : NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
        );
        meta.lore(List.of(Messages.get(loc, "admin.click-to-switch").decoration(TextDecoration.ITALIC, false)));
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack toggleItem(SupportedLocale loc, Material mat, String name, boolean enabled, List<Component> desc) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName((Component.text(name + " : ", NamedTextColor.AQUA)
                .append(Component.text(enabled ? "ON" : "OFF", enabled ? NamedTextColor.GREEN : NamedTextColor.RED)))
                .decoration(TextDecoration.ITALIC, false));

        var lore = new ArrayList<Component>();
        for (Component d : desc) lore.add(d.decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Messages.get(loc, "admin.click-to-toggle").decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack toggleChoiceItem(SupportedLocale loc, Material mat, String name, String valueLabel, int amount, List<Component> desc) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName((Component.text(name + " : ", NamedTextColor.AQUA)
                .append(Component.text(valueLabel, NamedTextColor.GREEN)))
                .decoration(TextDecoration.ITALIC, false));
        it.setAmount(Math.max(1, amount));

        var lore = new ArrayList<Component>();
        for (Component d : desc) lore.add(d.decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Messages.get(loc, "admin.click-to-toggle").decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack disabledItem(SupportedLocale loc, Material mat, String name, List<Component> reason) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();

        meta.displayName((Component.text(name + " : ", NamedTextColor.AQUA)
                .append(Component.text("LOCKED", NamedTextColor.RED)))
                .decoration(TextDecoration.ITALIC, false));

        var lore = new ArrayList<Component>();
        for (Component d : reason) lore.add(d.decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack placeholder(SupportedLocale loc, String nameKey, String loreKey) {
        ItemStack it = new ItemStack(Material.BARRIER);
        ItemMeta meta = it.getItemMeta();

        meta.displayName(Messages.get(loc, nameKey).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Messages.get(loc, loreKey).decoration(TextDecoration.ITALIC, false)));

        it.setItemMeta(meta);
        return it;
    }
}
