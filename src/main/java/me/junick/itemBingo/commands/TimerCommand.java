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
            sender.sendMessage("§c사용법: /timer <start|stop|pause> [초]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (args.length < 2) {
                    sender.sendMessage("§c사용법: /timer start <초>");
                    return true;
                }
                try {
                    int seconds = Integer.parseInt(args[1]);

                    TimerManager.start(seconds);
                    sender.sendMessage("§a타이머가 " + seconds + "초로 시작되었습니다.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c올바른 정수를 입력하세요.");
                }
            }
            case "pause" -> {
                boolean nowPaused = TimerManager.togglePause();
                sender.sendMessage(nowPaused ? "§a타이머가 일시정지되었습니다." : "§a타이머가 재개되었습니다.");
            }
            case "stop" -> {
                TimerManager.stop();
                sender.sendMessage("§a타이머가 정지되었습니다.");
            }
            default -> sender.sendMessage("§c알 수 없는 명령입니다. /timer <start|stop|pause> [초]");
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
