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

    public static final String TITLE_MAIN = "§8ItemBingo Admin";
    public static final String TITLE_TEAM = "§8Admin - Team";
    public static final String TITLE_SHOP = "§8Admin - Shop";
    public static final String TITLE_VANILLA = "§8Admin - Vanilla";
    public static final String TITLE_MODE = "§8Admin - Mode";

    public static void openMain(ItemBingo plugin, Player p) {
        openTeam(plugin, p);
    }

    public static void openTeam(ItemBingo plugin, Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_TEAM);

        placeTabs(inv, "TEAM");

        inv.setItem(13, toggleItem(
            Material.REDSTONE_TORCH,
                "팀 모드",
                Settings.isTeamEnabled(),
                List.of(Component.text("팀 모드 ON/OFF", NamedTextColor.GRAY))
        ));

        p.openInventory(inv);
    }

    public static void openShop(ItemBingo plugin, Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_SHOP);

        placeTabs(inv, "SHOP");

        boolean shopOn = Settings.isShopEnabled();

        inv.setItem(11, toggleItem(
                Material.EMERALD,
                "상점",
                shopOn,
                List.of(Component.text("상점 기능 전체 ON/OFF", NamedTextColor.GRAY))
        ));

        inv.setItem(13, shopOn
                ? toggleItem(Material.POTION, "이펙트 상점", Settings.isEffectShopEnabled(), List.of(Component.text("이펙트 상점 ON/OFF", NamedTextColor.GRAY)))
                : disabledItem(Material.GRAY_DYE, "이펙트 상점", List.of(Component.text("상점이 OFF라 변경할 수 없음", NamedTextColor.GRAY))));

        inv.setItem(15, shopOn
                ? toggleItem(Material.CHEST, "아이템 상점", Settings.isItemShopEnabled(), List.of(Component.text("아이템 상점 ON/OFF", NamedTextColor.GRAY)))
                : disabledItem(Material.GRAY_DYE, "아이템 상점", List.of(Component.text("상점이 OFF라 변경할 수 없음", NamedTextColor.GRAY))));

        p.openInventory(inv);
    }

    public static void openVanilla(ItemBingo plugin, Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_VANILLA);

        placeTabs(inv, "VANILLA");

        inv.setItem(13, toggleItem(
                Material.IRON_SHOVEL,
                "삽 우클릭으로 구리 산화",
                Settings.isShovelOxidizeCopper(),
                List.of(Component.text("삽으로 우클릭하면 구리를 1단계 산화시킴", NamedTextColor.GRAY), Component.text("(기본 OFF)", NamedTextColor.GRAY))
        ));

        inv.setItem(14, toggleChoiceItem(
                Material.IRON_SHOVEL,
                "패널티",
                Settings.getPenaltyDisplay(),
                Settings.getPenaltyInt() + 1,
                List.of(Component.text("1: 제출 합, 2: 마지막 제출, 3: 점수제", NamedTextColor.GRAY), Component.text("(기본 1: 제출 합)", NamedTextColor.GRAY))
        ));

        inv.setItem(22, placeholder("기타등등", "나중에 더 추가할 것"));

        p.openInventory(inv);
    }

    public static void openMode(ItemBingo plugin, Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_MODE);

        placeTabs(inv, "MODE");

        // First functional row: the mode switch button.
        inv.setItem(13, modeSwitchItem());

        // Second row onwards: settings specific to the currently selected mode.
        switch (Settings.getGameMode()) {
            case NORMAL -> inv.setItem(22, placeholder(
                    "추가 설정 없음", "일반 모드에는 별도 설정이 없습니다"));

            case SWAPPAGE -> {
                inv.setItem(21, toggleItem(
                        Material.CLOCK,
                        "스왑 타이머",
                        Settings.isSwapTimer(),
                        List.of(Component.text("스왑 타이머 표시?", NamedTextColor.GRAY), Component.text("(기본 OFF)", NamedTextColor.GRAY))
                ));
                inv.setItem(23, toggleItem(
                        Material.ENDER_PEARL,
                        "스왑 경고",
                        Settings.isSwapAlert(),
                        List.of(Component.text("바뀌기 3초 전에 알림?", NamedTextColor.GRAY), Component.text("(기본 OFF)", NamedTextColor.GRAY))
                ));
            }

            case FOG_OF_WAR -> {
                inv.setItem(20, toggleItem(
                        Material.BARRIER,
                        "공개된 칸만 제출",
                        Settings.isFogSubmitLock(),
                        List.of(Component.text("공개되지 않은 칸은 제출 불가", NamedTextColor.GRAY), Component.text("(기본 OFF)", NamedTextColor.GRAY))
                ));
                inv.setItem(22, toggleItem(
                        Material.AMETHYST_SHARD,
                        "칸 공개 알림",
                        Settings.isFogRevealAlert(),
                        List.of(Component.text("새 칸이 공개되면 소리/메시지", NamedTextColor.GRAY), Component.text("(기본 ON)", NamedTextColor.GRAY))
                ));
                inv.setItem(24, toggleItem(
                        Material.RECOVERY_COMPASS,
                        "대각선 공개",
                        Settings.isFogDiagonalReveal(),
                        List.of(Component.text("제출 시 대각선 칸까지 공개", NamedTextColor.GRAY), Component.text("(기본 OFF)", NamedTextColor.GRAY))
                ));
            }
        }

        p.openInventory(inv);
    }

    /** The mode switch button — clicking it cycles Normal → Swappage → Fog of War. */
    private static ItemStack modeSwitchItem() {
        Settings.GameMode mode = Settings.getGameMode();
        Material mat = switch (mode) {
            case NORMAL -> Material.WHITE_WOOL;
            case SWAPPAGE -> Material.ENDER_EYE;
            case FOG_OF_WAR -> Material.LIGHT_GRAY_STAINED_GLASS;
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

    private static void placeTabs(Inventory inv, String selected) {
        inv.setItem(0, tabItem(Material.WHITE_BANNER, "팀 관련", selected.equals("TEAM")));
        inv.setItem(1, tabItem(Material.GREEN_BANNER, "상점 관련", selected.equals("SHOP")));
        inv.setItem(2, tabItem(Material.ORANGE_BANNER, "바닐라 관련", selected.equals("VANILLA")));
        inv.setItem(3, tabItem(Material.PURPLE_BANNER, "모드 관련", selected.equals("MODE")));

        for (int i = 4; i < 9; i++) {
            inv.setItem(i, filler());
        }
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
