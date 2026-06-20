package me.junick.itemBingo.commands;

import me.junick.itemBingo.enums.BingoRewardType;
import me.junick.itemBingo.util.PlayerDataManager;
import me.junick.itemBingo.util.ProgressFactory;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PointCommand
implements CommandExecutor, TabCompleter {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length != 3) {
            sender.sendMessage("§c사용법: /pointadd <플레이어> <diamond|line|slot> <수량>");
            return true;
        }
        var p = Bukkit.getPlayer(args[0]);
        if (p == null) {
            sender.sendMessage("§c플레이어를 찾을 수 없습니다: " + args[0]);
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        }
        catch (NumberFormatException e) {
            sender.sendMessage("§c올바른 숫자를 입력하세요.");
            return true;
        }
        var prog = ProgressFactory.of(p);
        switch (args[1].toLowerCase()) {
            case "diamond" -> prog.addCurrency(p, BingoRewardType.DIAMOND, amount);
            case "line" -> prog.addCurrency(p, BingoRewardType.LINE, amount);
            case "slot" -> prog.addCurrency(p, BingoRewardType.SLOT, amount);
        }
        sender.sendMessage(String.format("%s에게 %s %d개를 주었다", p.getName(), args[1].toLowerCase(), amount));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerNames.add(player.getName());
            }
            StringUtil.copyPartialMatches(args[0], playerNames, completions);
        } else if (args.length == 2) {
            completions.add("diamond");
            completions.add("line");
            completions.add("slot");
        }
        return completions;
    }
}

