package me.junick.itemBingo.commands;

import me.junick.itemBingo.util.ChatManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /ac <message>} — sends a one-off message to all chat regardless of the
 * sender's current {@link ChatManager} channel.
 */
public class AllChatCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }
        if (args.length == 0) {
            p.sendMessage("§c사용법: /ac <내용>");
            return true;
        }

        ChatManager.sendAll(p, Component.text(String.join(" ", args)));
        return true;
    }
}
