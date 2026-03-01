package me.junick.itemBingo.commands;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.BundleManager;
import me.junick.itemBingo.model.BingoBoard;
import me.junick.itemBingo.util.BingoItemSelector;
import me.junick.itemBingo.util.BingoStorage;
import me.junick.itemBingo.util.PlayerDataManager;
import me.junick.itemBingo.util.TeamDataManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RollBingo implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
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

            List<ItemStack> items = BingoItemSelector.getWeightedRandomSurvivalItems(count);

            BundleManager.resetPlayerList();
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

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (int i = 1; i <= 9; i++) completions.add(String.valueOf(i));
        }
        if (args.length == 2) {
            for (int i = 1; i <= 6; i++) completions.add(String.valueOf(i));
        }
        return completions;
    }
}
