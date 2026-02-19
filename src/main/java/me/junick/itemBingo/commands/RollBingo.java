package me.junick.itemBingo.commands;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.model.BingoBoard;
import me.junick.itemBingo.util.BingoItemSelector;
import me.junick.itemBingo.util.BingoStorage;
import me.junick.itemBingo.util.PlayerDataManager;
import me.junick.itemBingo.util.TeamDataManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class RollBingo implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("§cUsage: /rollbingo <width> <height>");
            return true;
        }

        try {
            int width = Integer.parseInt(args[0]);
            int height = Integer.parseInt(args[1]);

            if (width < 1 || width > 9 || height < 1 || height > 6) {
                sender.sendMessage("§cWidth must be between 1 and 9, and height must be between 1 and 6.");
                return true;
            }

            int count = width * height;

            List<ItemStack> items = BingoItemSelector.getRandomSurvivalItems(count);

            PlayerDataManager.resetAll();
            TeamDataManager.resetAll();

            BingoBoard board = new BingoBoard(width, height, items);
            ItemBingo.currentBingo = board;
            BingoStorage.save(board);

            sender.sendMessage("§aBingo board rolled with " + width + "x" + height + " items!");
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid width or height! Please enter valid integers.");
        }
        return true;
    }
}
