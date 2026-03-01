package me.junick.itemBingo.commands;

import me.junick.itemBingo.enums.BingoItem;
import me.junick.itemBingo.util.CustomItems;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class CustomItemCommand implements CommandExecutor, Listener {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }
        Inventory inv = Bukkit.createInventory(null, 54, "커스텀 아이템");
        inv.addItem();
        var copper = CustomItems.get(BingoItem.COPPER_OXIDIZER);
        copper.setAmount(64);
        var filler = CustomItems.get(BingoItem.BINGO_FILLER);
        filler.setAmount(64);
        var dye = CustomItems.get(BingoItem.DYE_SELECTOR);
        dye.setAmount(64);
        inv.addItem(copper);
        inv.addItem(filler);
        inv.addItem(dye);
        inv.addItem(CustomItems.get(BingoItem.EXPLORER_MAP));
        inv.addItem(CustomItems.get(BingoItem.EXPLORER_MAP));
        inv.addItem(CustomItems.get(BingoItem.EXPLORER_MAP));
        inv.addItem(CustomItems.get(BingoItem.BIOME_MAP));
        inv.addItem(CustomItems.get(BingoItem.BIOME_MAP));
        inv.addItem(CustomItems.get(BingoItem.BIOME_MAP));
        p.openInventory(inv);
        return true;
    }

}