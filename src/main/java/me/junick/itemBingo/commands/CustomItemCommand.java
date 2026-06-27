package me.junick.itemBingo.commands;

import me.junick.itemBingo.enums.BingoItem;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
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
            sender.sendMessage(Messages.get(sender, "command.players-only"));
            return true;
        }
        SupportedLocale loc = Messages.localeOf(p);
        Inventory inv = Bukkit.createInventory(null, 54, Messages.get(loc, "command.customitem.title"));
        var copper = CustomItems.get(BingoItem.COPPER_OXIDIZER, loc);
        copper.setAmount(64);
        var filler = CustomItems.get(BingoItem.BINGO_FILLER, loc);
        filler.setAmount(64);
        var dye = CustomItems.get(BingoItem.DYE_SELECTOR, loc);
        dye.setAmount(64);
        inv.addItem(copper);
        inv.addItem(filler);
        inv.addItem(dye);
        inv.addItem(CustomItems.get(BingoItem.EXPLORER_MAP, loc));
        inv.addItem(CustomItems.get(BingoItem.EXPLORER_MAP, loc));
        inv.addItem(CustomItems.get(BingoItem.EXPLORER_MAP, loc));
        inv.addItem(CustomItems.get(BingoItem.BIOME_MAP, loc));
        inv.addItem(CustomItems.get(BingoItem.BIOME_MAP, loc));
        inv.addItem(CustomItems.get(BingoItem.BIOME_MAP, loc));
        p.openInventory(inv);
        return true;
    }

}
