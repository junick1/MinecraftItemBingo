package me.junick.itemBingo.commands;

import me.junick.itemBingo.util.TimerManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TimerCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
}
