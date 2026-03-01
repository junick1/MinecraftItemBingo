package me.junick.itemBingo.commands;

import me.junick.itemBingo.util.TimerManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TimerCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /timer <start|stop|pause> [seconds]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /timer start <seconds>");
                    return true;
                }
                try {
                    int seconds = Integer.parseInt(args[1]);

                    TimerManager.start(seconds);
                    sender.sendMessage("§aTimer started for " + seconds + " seconds.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid number of seconds. Please enter a valid integer.");
                }
            }
            case "pause" -> {
                boolean nowPaused = TimerManager.togglePause();
                sender.sendMessage(nowPaused ? "§aTimer paused." : "§aTimer resumed.");
            }
            case "stop" -> {
                TimerManager.stop();
                sender.sendMessage("§aTimer stopped.");
            }
            default -> sender.sendMessage("§cInvalid command. Use /timer <start|stop|pause> [seconds].");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("start");
            completions.add("stop");
            completions.add("pause");
        }
        if (args.length == 2 && args[1].equals("start")) {
            completions.add("1800");
            completions.add("2700");
            completions.add("3600");
            completions.add("5400");
        }
        return completions;
    }
}
