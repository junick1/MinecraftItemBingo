package me.junick.itemBingo.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.enums.BingoItemTag;
import me.junick.itemBingo.interfaces.access.BingoProgressAccess;
import me.junick.itemBingo.interfaces.access.SoloProgressAccess;
import me.junick.itemBingo.model.BingoBoard;
import me.junick.itemBingo.model.PlayerBingoProgress;
import me.junick.itemBingo.util.BingoTagLoader;
import me.junick.itemBingo.util.PlayerDataManager;
import me.junick.itemBingo.util.ProgressFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class BingoGUI {
    private static final int GUI_WIDTH = 9;
    private static final int MAX_HEIGHT = 6;

    public static final String TITLE = "§f빙고판";

    public static void open(Player p) {
        BingoBoard board = ItemBingo.currentBingo;
        if (board == null) {
            p.sendMessage("§c현재 빙고판이 없습니다.");
            return;
        }

        BingoProgressAccess progress = ProgressFactory.of(p);

        int boardWidth = board.getWidth();
        int boardHeight = board.getHeight();
        boolean tightMode = boardHeight > 4;

        int guiHeight = tightMode ? boardHeight : boardHeight + 2;
        if (guiHeight > MAX_HEIGHT) {
            p.sendMessage(Component.text("GUI 높이가 " + MAX_HEIGHT + "줄을 초과합니다.", NamedTextColor.RED));
            return;
        }

        Inventory inv = Bukkit.createInventory(
                null,
                GUI_WIDTH * guiHeight,
                TITLE
        );

        fillBackground(inv);
        placeBingoItems(inv, board, progress, tightMode);

        p.openInventory(inv);
    }

    private static void fillBackground(Inventory inv) {
        ItemStack filler = createPane(Material.GRAY_STAINED_GLASS_PANE, null);

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }

    private static void placeBingoItems(
            Inventory inv,
            BingoBoard board,
            BingoProgressAccess progress,
            boolean tightMode
    ) {
        List<ItemStack> items = board.getItems();
        BingoTagLoader tagLoader = ItemBingo.getInstance().getTagLoader();

        int offsetX = (GUI_WIDTH - board.getWidth()) / 2;
        int offsetY = tightMode ? 0 : 1;

        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                int index = y * board.getWidth() + x;
                if (index >= items.size()) continue;

                int slot = (y + offsetY) * GUI_WIDTH + offsetX + x;

                if (progress.isSubmitted(index)) {
                    inv.setItem(slot, submittedIcon(items.get(index)));
                } else {
                    inv.setItem(slot, withTagLore(items.get(index), tagLoader));
                }
            }
        }
    }

    /**
     * The icon shown for an already-submitted slot: the real item, glowing, with
     * a green "제출됨" name. Both the initial render and live updates after a
     * submission go through here so a submitted slot always looks the same.
     */
    public static ItemStack submittedIcon(ItemStack original) {
//        ItemStack item = original.clone();
        ItemStack item = new ItemStack(Material.BARRIER);
        item.setAmount(1);

        ItemMeta meta = item.getItemMeta();


        meta.displayName(Component.text("✔ 제출됨", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        meta.lore(List.of(Component.text("이미 제출한 칸입니다.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));

        meta.setEnchantmentGlintOverride(true);
        meta.addItemFlags(
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_DYE,
                ItemFlag.HIDE_ARMOR_TRIM
        );

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createPane(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (name != null) {
            meta.displayName(name);
        } else {
            meta.setHideTooltip(true);
        }

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack withTagLore(ItemStack original, BingoTagLoader tagLoader) {
        ItemStack item = original.clone();
        ItemMeta meta = item.getItemMeta();

        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_DYE,
                ItemFlag.HIDE_ARMOR_TRIM
        );

        EnumSet<BingoItemTag> tags = tagLoader.getTags(item.getType());
        if (tags.isEmpty()) return item;

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        for (BingoItemTag tag : tags) {
            lore.add(tag.bullet());
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
