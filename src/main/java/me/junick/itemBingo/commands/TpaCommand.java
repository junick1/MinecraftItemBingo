package me.junick.itemBingo.commands;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.util.TpaManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /tpa &lt;player&gt; — request teleport to a teammate. Tab-complete lists only
 * online teammates of the sender.
 */
public class TpaCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Messages.get(sender, "command.players-only"));
            return true;
        }

        if (!Settings.isTpaEffective()) {
            p.sendMessage(Messages.get(p, "command.tpa.disabled"));
            return true;
        }

        if (args.length != 1) {
            p.sendMessage(Messages.get(p, "command.tpa.usage"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            p.sendMessage(Messages.get(p, "command.tpa.offline", "player", args[0]));
            return true;
        }

        TpaManager.request(p, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, String[] args) {
        if (!(sender instanceof Player p)) return Collections.emptyList();
        if (!Settings.isTpaEffective()) return Collections.emptyList();
        if (args.length != 1) return Collections.emptyList();

        String prefix = args[0].toLowerCase();
        return ItemBingo.getInstance().getTeamManager().getOnlineTeammates(p).stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(prefix))
                .collect(Collectors.toList());
    }
}
