package me.junick.itemBingo.commands;

import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.util.ChatManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /tc <message>} — sends a one-off message to team chat regardless of the
 * sender's current {@link ChatManager} channel. If the sender has no team the
 * message is blocked with a notice (handled by {@link ChatManager#sendTeam}).
 */
public class TeamChatCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Messages.get(sender, "command.players-only"));
            return true;
        }
        if (args.length == 0) {
            p.sendMessage(Messages.get(p, "command.tc.usage"));
            return true;
        }

        ChatManager.sendTeam(p, Component.text(String.join(" ", args)));
        return true;
    }
}
