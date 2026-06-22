package me.junick.itemBingo.commands;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.util.ChatManager;
import me.junick.itemBingo.util.ChatManager.Channel;
import me.junick.itemBingo.util.TeamManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /chat <team|t|all|a>} — sets the player's default chat channel. The choice
 * persists across restarts ({@link ChatManager}). One-off messages to the other
 * channel are sent with {@code /ac} and {@code /tc} without changing this setting.
 */
public class ChatCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (args.length == 0) {
            Channel current = ChatManager.getChannel(p);
            p.sendMessage("§e현재 채팅 채널: §f" + (current == Channel.TEAM ? "팀" : "전체"));
            p.sendMessage("§7/chat team §8또는 §7/chat all §8로 변경하세요.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "t", "team" -> {
                ChatManager.setChannel(p, Channel.TEAM);
                p.sendMessage("§a이제 §2팀 채팅§a으로 대화합니다.");
                if (ItemBingo.getInstance().getTeamManager().effectiveTeamId(p) == TeamManager.NO_TEAM) {
                    p.sendMessage("§e주의: 현재 팀이 없어 메시지가 전송되지 않습니다.");
                }
            }
            case "a", "all" -> {
                ChatManager.setChannel(p, Channel.ALL);
                p.sendMessage("§a이제 §f전체 채팅§a으로 대화합니다.");
            }
            default -> p.sendMessage("§c사용법: /chat <team|t|all|a>");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String option : List.of("team", "all")) {
                if (option.startsWith(prefix)) out.add(option);
            }
        }
        return out;
    }
}
