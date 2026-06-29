package me.junick.itemBingo.commands;

import me.junick.itemBingo.i18n.Messages;
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
            sender.sendMessage(Messages.get(sender, "command.timer.usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (args.length < 2) {
                    sender.sendMessage(Messages.get(sender, "command.timer.start-usage"));
                    return true;
                }
                try {
                    int seconds = Integer.parseInt(args[1]);

                    TimerManager.start(seconds);
                    sender.sendMessage(Messages.get(sender, "command.timer.started", "seconds", seconds));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Messages.get(sender, "command.invalid-number"));
                }
            }
            case "pause" -> {
                boolean nowPaused = TimerManager.togglePause();
                sender.sendMessage(Messages.get(sender, nowPaused ? "command.timer.paused" : "command.timer.resumed"));
            }
            case "stop" -> {
                TimerManager.stop();
                sender.sendMessage(Messages.get(sender, "command.timer.stopped"));
            }
            default -> sender.sendMessage(Messages.get(sender, "command.timer.unknown"));
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
