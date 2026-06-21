package me.junick.itemBingo.commands;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.BundleManager;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.util.ConfirmationManager;
import me.junick.itemBingo.util.TeamManager;
import me.junick.itemBingo.util.TimerManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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
            sender.sendMessage("§c사용법: /startbingo <초>");
            return true;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c올바른 정수를 입력하세요.");
            return true;
        }

        if (!ConfirmationManager.confirm(sender, "startbingo")) {
            sender.sendMessage("§c§l[경고] §f게임을 시작하면 모든 플레이어의 인벤토리가 비워지고 리스폰됩니다.");

            // In team mode, warn about players who haven't been assigned to a team
            // (OPs excluded) — they won't be able to see or submit to the board.
            if (Settings.isTeamEnabled()) {
                TeamManager tm = ItemBingo.getInstance().getTeamManager();
                List<Player> unassigned = tm.getUnassignedOnlinePlayers();
                if (!unassigned.isEmpty()) {
                    String names = unassigned.stream().map(Player::getName).collect(java.util.stream.Collectors.joining(", "));
                    sender.sendMessage("§c§l[경고] §f팀에 배정되지 않은 플레이어가 있습니다: §e" + names);
                }
            }

            sender.sendMessage("§e계속하려면 10초 안에 §f/startbingo " + seconds + "§e을(를) 다시 입력하세요.");
            return true;
        }

        TimerManager.start(seconds);
        BundleManager.resetPlayerList();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().clear();
            player.setHealth(0);
            player.setGameMode(GameMode.SURVIVAL);
            BundleManager.getBundle(player);
        }

        sender.sendMessage("§a빙고가 " + seconds + "초 동안 시작되었습니다.");
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
