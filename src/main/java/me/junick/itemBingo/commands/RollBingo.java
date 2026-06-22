package me.junick.itemBingo.commands;

import me.junick.itemBingo.ItemBingo;
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
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("§c사용법: /rollbingo <가로> <세로>");
            return true;
        }

        try {
            int width = Integer.parseInt(args[0]);
            int height = Integer.parseInt(args[1]);

            if (width < 1 || width > 9 || height < 1 || height > 6) {
                sender.sendMessage("§c가로는 1~9, 세로는 1~6 사이여야 합니다.");
                return true;
            }

            if (!ConfirmationManager.confirm(sender, "rollbingo")) {
                sender.sendMessage("§c§l[경고] §f새 빙고판을 생성하면 모든 플레이어와 팀의 진행 데이터가 초기화됩니다.");
                sender.sendMessage("§e계속하려면 10초 안에 §f/rollbingo " + width + " " + height + "§e을(를) 다시 입력하세요.");
                return true;
            }

            int count = width * height;

            List<ItemStack> items = BingoItemSelector.getWeightedRandomSurvivalItems(count);

            ItemBingo.applyNewBoard(new BingoBoard(width, height, items));

            sender.sendMessage("§a" + width + "x" + height + " 빙고판이 생성되었습니다!");
        } catch (NumberFormatException e) {
            sender.sendMessage("§c올바른 정수를 입력하세요.");
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
