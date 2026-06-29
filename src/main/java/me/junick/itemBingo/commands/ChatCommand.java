package me.junick.itemBingo.commands;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.i18n.Messages;
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
            sender.sendMessage(Messages.get(sender, "command.players-only"));
            return true;
        }

        if (args.length == 0) {
            Channel current = ChatManager.getChannel(p);
            String channel = Messages.legacy(p, current == Channel.TEAM
                    ? "command.chat.channel-team" : "command.chat.channel-all");
            p.sendMessage(Messages.get(p, "command.chat.current", "channel", channel));
            p.sendMessage(Messages.get(p, "command.chat.hint"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "t", "team" -> {
                ChatManager.setChannel(p, Channel.TEAM);
                p.sendMessage(Messages.get(p, "command.chat.now-team"));
                if (ItemBingo.getInstance().getTeamManager().effectiveTeamId(p) == TeamManager.NO_TEAM) {
                    p.sendMessage(Messages.get(p, "command.chat.no-team-warn"));
                }
            }
            case "a", "all" -> {
                ChatManager.setChannel(p, Channel.ALL);
                p.sendMessage(Messages.get(p, "command.chat.now-all"));
            }
            default -> p.sendMessage(Messages.get(p, "command.chat.usage"));
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
