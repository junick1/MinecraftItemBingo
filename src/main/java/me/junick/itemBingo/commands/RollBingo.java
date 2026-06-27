package me.junick.itemBingo.commands;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.model.BingoBoard;
import me.junick.itemBingo.util.BingoItemSelector;
import me.junick.itemBingo.util.ConfirmationManager;
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
    // Boards beyond 9x6 no longer fit the inventory directly; BingoGUI renders
    // them through a scrollable viewport, so the roll cap can be much larger.
    private static final int MAX_WIDTH = 25;
    private static final int MAX_HEIGHT = 25;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(Messages.get(sender, "command.rollbingo.usage"));
            return true;
        }

        try {
            int width = Integer.parseInt(args[0]);
            int height = Integer.parseInt(args[1]);

            if (width < 1 || width > MAX_WIDTH || height < 1 || height > MAX_HEIGHT) {
                sender.sendMessage(Messages.get(sender, "command.rollbingo.range",
                        "maxw", MAX_WIDTH, "maxh", MAX_HEIGHT));
                return true;
            }

            if (!ConfirmationManager.confirm(sender, "rollbingo")) {
                sender.sendMessage(Messages.get(sender, "command.rollbingo.warn"));
                sender.sendMessage(Messages.get(sender, "command.rollbingo.confirm",
                        "width", width, "height", height));
                return true;
            }

            int count = width * height;

            List<ItemStack> items = BingoItemSelector.getWeightedRandomSurvivalItems(count);

            ItemBingo.applyNewBoard(new BingoBoard(width, height, items));

            sender.sendMessage(Messages.get(sender, "command.rollbingo.created",
                    "width", width, "height", height));
        } catch (NumberFormatException e) {
            sender.sendMessage(Messages.get(sender, "command.invalid-number"));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (int i = 1; i <= MAX_WIDTH; i++) completions.add(String.valueOf(i));
        }
        if (args.length == 2) {
            for (int i = 1; i <= MAX_HEIGHT; i++) completions.add(String.valueOf(i));
        }
        return completions;
    }
}
