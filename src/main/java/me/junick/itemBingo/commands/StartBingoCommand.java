package me.junick.itemBingo.commands;

import me.junick.itemBingo.util.TimerManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class StartBingoCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /startbingo <seconds>");
            return true;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number of seconds. Please enter a valid integer.");
            return true;
        }

        TimerManager.start(seconds);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().clear();
            player.setHealth(0);
        }

        sender.sendMessage("§aBingo started for " + seconds + " seconds.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("1800");
            completions.add("2700");
            completions.add("3600");
            completions.add("5400");
        }
        return completions;
    }
}
