package me.junick.itemBingo.admin;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class AdminGUI {

    /** Every admin tab is the same size for a consistent, polished look. */
    public static final int SIZE = 54;

    public static final String TITLE_MAIN = "§8ItemBingo Admin";
    public static final String TITLE_GAME = "§8Admin - Game";
    public static final String TITLE_SHOP = "§8Admin - Shop";
    public static final String TITLE_VANILLA = "§8Admin - Vanilla";
    public static final String TITLE_MODE = "§8Admin - Mode";

    public static void openMain(ItemBingo plugin, Player p) {
        openGame(plugin, p);
    }

    public static void openGame(ItemBingo plugin, Player p) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE_GAME);
        decorate(inv, "GAME");

        // Team mode on/off.
        inv.setItem(20, toggleItem(
                Material.REDSTONE_TORCH,
                "팀 모드",
                Settings.isTeamEnabled(),
                List.of(Component.text("팀 모드 ON/OFF", NamedTextColor.GRAY))
        ));

        // Shared/private storage chest capacity.
        inv.setItem(22, chestCapacityItem());

        // /tpa — only meaningful when team mode is on.
        inv.setItem(24, tpaItem());

        // Score calculation (penalty) — moved here from the Vanilla tab.
        inv.setItem(31, toggleChoiceItem(
                Material.COMPARATOR,
                "점수 계산 방식",
                Settings.getPenaltyDisplay(),
                Settings.getPenaltyInt() + 1,
                List.of(Component.text("1: 제출 합, 2: 마지막 제출, 3: 점수제", NamedTextColor.GRAY), Component.text("(기본 1: 제출 합)", NamedTextColor.GRAY))
        ));

        p.openInventory(inv);
    }

    public static void openShop(ItemBingo plugin, Player p) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE_SHOP);
        decorate(inv, "SHOP");

        boolean shopOn = Settings.isShopEnabled();

        inv.setItem(20, toggleItem(
                Material.EMERALD,
                "상점",
                shopOn,
                List.of(Component.text("상점 기능 전체 ON/OFF", NamedTextColor.GRAY))
        ));

        inv.setItem(22, shopOn
                ? toggleItem(Material.POTION, "이펙트 상점", Settings.isEffectShopEnabled(), List.of(Component.text("이펙트 상점 ON/OFF", NamedTextColor.GRAY)))
                : disabledItem(Material.GRAY_DYE, "이펙트 상점", List.of(Component.text("상점이 OFF라 변경할 수 없음", NamedTextColor.GRAY))));

        inv.setItem(24, shopOn
                ? toggleItem(Material.CHEST, "아이템 상점", Settings.isItemShopEnabled(), List.of(Component.text("아이템 상점 ON/OFF", NamedTextColor.GRAY)))
                : disabledItem(Material.GRAY_DYE, "아이템 상점", List.of(Component.text("상점이 OFF라 변경할 수 없음", NamedTextColor.GRAY))));

        p.openInventory(inv);
    }

    public static void openVanilla(ItemBingo plugin, Player p) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE_VANILLA);
        decorate(inv, "VANILLA");

        inv.setItem(22, toggleItem(
                Material.IRON_SHOVEL,
                "삽 우클릭으로 구리 산화",
                Settings.isShovelOxidizeCopper(),
                List.of(Component.text("삽으로 우클릭하면 구리를 1단계 산화시킴", NamedTextColor.GRAY), Component.text("(기본 OFF)", NamedTextColor.GRAY))
        ));

        inv.setItem(31, placeholder("기타등등", "나중에 더 추가할 것"));

        p.openInventory(inv);
    }

    public static void openMode(ItemBingo plugin, Player p) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE_MODE);
        decorate(inv, "MODE");

        // The mode switch button.
        inv.setItem(20, modeSwitchItem());

        // Settings specific to the currently selected mode.
        switch (Settings.getGameMode()) {
            case NORMAL -> inv.setItem(31, placeholder(
                    "추가 설정 없음", "일반 모드에는 별도 설정이 없습니다"));

            case LOCKOUT -> inv.setItem(31, placeholder(
                    "추가 설정 없음", "선점 모드에는 별도 설정이 없습니다"));

            case SWAPPAGE -> {
                inv.setItem(30, toggleItem(
                        Material.CLOCK,
                        "스왑 타이머",
                        Settings.isSwapTimer(),
                        List.of(Component.text("스왑 타이머 표시?", NamedTextColor.GRAY), Component.text("(기본 OFF)", NamedTextColor.GRAY))
                ));
                inv.setItem(32, toggleItem(
                        Material.ENDER_PEARL,
                        "스왑 경고",
                        Settings.isSwapAlert(),
                        List.of(Component.text("바뀌기 3초 전에 알림?", NamedTextColor.GRAY), Component.text("(기본 OFF)", NamedTextColor.GRAY))
                ));
            }

            case FOG_OF_WAR -> {
                inv.setItem(29, toggleItem(
                        Material.BARRIER,
                        "공개된 칸만 제출",
                        Settings.isFogSubmitLock(),
                        List.of(Component.text("공개되지 않은 칸은 제출 불가", NamedTextColor.GRAY), Component.text("(기본 OFF)", NamedTextColor.GRAY))
                ));
                inv.setItem(31, toggleItem(
                        Material.AMETHYST_SHARD,
                        "칸 공개 알림",
                        Settings.isFogRevealAlert(),
                        List.of(Component.text("새 칸이 공개되면 소리/메시지", NamedTextColor.GRAY), Component.text("(기본 ON)", NamedTextColor.GRAY))
                ));
                inv.setItem(33, toggleItem(
                        Material.RECOVERY_COMPASS,
                        "대각선 공개",
                        Settings.isFogDiagonalReveal(),
                        List.of(Component.text("제출 시 대각선 칸까지 공개", NamedTextColor.GRAY), Component.text("(기본 OFF)", NamedTextColor.GRAY))
                ));
            }
        }

        p.openInventory(inv);
    }

    /**
     * The storage-chest capacity control. Same ender-chest icon regardless of
     * team mode; only the label switches between "공유 창고" (shared) and "개인 창고"
     * (private). Left-click raises rows (cycling back to disabled), right-click
     * lowers them (cycling back to 6).
     */
    private static ItemStack chestCapacityItem() {
        int rows = Settings.getChestRows();
        String label = Settings.isTeamEnabled() ? "공유 창고 용량" : "개인 창고 용량";
        String value = rows == 0 ? "비활성화" : rows + "줄";

        ItemStack it = new ItemStack(Material.ENDER_CHEST);
        it.setAmount(Math.max(1, rows));
        ItemMeta meta = it.getItemMeta();
        meta.displayName((Component.text(label + " : ", NamedTextColor.AQUA)
                .append(Component.text(value, rows == 0 ? NamedTextColor.RED : NamedTextColor.GREEN)))
                .decoration(TextDecoration.ITALIC, false));

        var lore = new java.util.ArrayList<Component>();
        lore.add(Component.text(Settings.isTeamEnabled()
                ? "팀이 함께 사용하는 공유 창고" : "플레이어 개인 전용 창고", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("비활성화, 1줄 ~ 6줄", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("좌클릭: 늘리기", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("우클릭: 줄이기", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        it.setItemMeta(meta);
        return it;
    }

    /**
     * The /tpa toggle. TPA only works while team mode is ON, so when team mode is
     * OFF this shows DISABLED (distinct from a normal OFF) while preserving the
     * stored value underneath.
     */
    private static ItemStack tpaItem() {
        ItemStack it = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = it.getItemMeta();

        if (!Settings.isTeamEnabled()) {
            meta.displayName((Component.text("/tpa : ", NamedTextColor.AQUA)
                    .append(Component.text("DISABLED", NamedTextColor.DARK_GRAY)))
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("팀원에게 텔레포트 요청", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("팀 모드가 켜져야 사용할 수 있습니다", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            it.setItemMeta(meta);
            return it;
        }

        boolean on = Settings.isTpaEnabled();
        meta.displayName((Component.text("/tpa : ", NamedTextColor.AQUA)
                .append(Component.text(on ? "ON" : "OFF", on ? NamedTextColor.GREEN : NamedTextColor.RED)))
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("팀원에게 텔레포트 요청", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("클릭해서 토글", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
        ));
        it.setItemMeta(meta);
        return it;
    }

    /** The mode switch button — clicking it cycles Normal → Swappage → Fog of War → Lockout. */
    private static ItemStack modeSwitchItem() {
        Settings.GameMode mode = Settings.getGameMode();
        Material mat = switch (mode) {
            case NORMAL -> Material.WHITE_WOOL;
            case SWAPPAGE -> Material.ENDER_EYE;
            case FOG_OF_WAR -> Material.LIGHT_GRAY_STAINED_GLASS;
            case LOCKOUT -> Material.IRON_BARS;
        };

        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName((Component.text("게임 모드 : ", NamedTextColor.AQUA)
                .append(Component.text(mode.getDisplay(), NamedTextColor.GREEN)))
                .decoration(TextDecoration.ITALIC, false));

        var lore = new java.util.ArrayList<Component>();
        for (Settings.GameMode m : Settings.GameMode.values()) {
            boolean cur = (m == mode);
            lore.add(Component.text((cur ? "▶ " : "   ") + m.getDisplay(),
                    cur ? NamedTextColor.GOLD : NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text("클릭해서 다음 모드로 전환", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        it.setItemMeta(meta);
        return it;
    }

    /** Fills the inventory with the background frame and lays out the tab row. */
    private static void decorate(Inventory inv, String selected) {
        ItemStack f = filler();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, f);
        }

        inv.setItem(0, tabItem(Material.WHITE_BANNER, "게임", selected.equals("GAME")));
        inv.setItem(1, tabItem(Material.GREEN_BANNER, "상점 관련", selected.equals("SHOP")));
        inv.setItem(2, tabItem(Material.ORANGE_BANNER, "바닐라 관련", selected.equals("VANILLA")));
        inv.setItem(3, tabItem(Material.PURPLE_BANNER, "모드 관련", selected.equals("MODE")));
    }

    private static ItemStack filler() {
        ItemStack it = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = it.getItemMeta();
        meta.setHideTooltip(true);
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack tabItem(Material mat, String name, boolean selected) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(
                Component.text((selected ? "▶ " : "") + name, selected ? NamedTextColor.GOLD : NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
        );
        meta.lore(List.of(Component.text("클릭해서 이동", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack toggleItem(Material mat, String name, boolean enabled, List<Component> desc) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName((Component.text(name + " : ", NamedTextColor.AQUA).append(Component.text(enabled ? "ON" : "OFF", enabled ? NamedTextColor.GREEN : NamedTextColor.RED))).decoration(TextDecoration.ITALIC, false));

        var lore = new java.util.ArrayList<Component>();
        for (Component d : desc) {
            lore.add(d.decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(Component.text("클릭해서 토글", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        it.setItemMeta(meta);
        return it;
    }

    /**
     * A multi-state toggle that shows the selected option's label (e.g. the
     * penalty mode name) rather than a raw number. {@code amount} controls the
     * stack size used as a subtle visual indicator of the current option.
     */
    private static ItemStack toggleChoiceItem(Material mat, String name, String valueLabel, int amount, List<Component> desc) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName((Component.text(name + " : ", NamedTextColor.AQUA).append(Component.text(valueLabel, NamedTextColor.GREEN))).decoration(TextDecoration.ITALIC, false));
        it.setAmount(Math.max(1, amount));

        var lore = new java.util.ArrayList<Component>();
        for (Component d : desc) {
            lore.add(d.decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(Component.text("클릭해서 토글", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack disabledItem(Material mat, String name, List<Component> reason) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();

        meta.displayName((Component.text(name + " : ", NamedTextColor.AQUA).append(Component.text("LOCKED", NamedTextColor.RED))).decoration(TextDecoration.ITALIC, false));

        var lore = new java.util.ArrayList<Component>();
        for (Component d : reason) {
            lore.add(d.decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);

        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack placeholder(String name, String loreLine) {
        ItemStack it = new ItemStack(Material.BARRIER);
        ItemMeta meta = it.getItemMeta();

        meta.displayName(Component.text(name, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text(loreLine, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));

        it.setItemMeta(meta);
        return it;
    }
}
