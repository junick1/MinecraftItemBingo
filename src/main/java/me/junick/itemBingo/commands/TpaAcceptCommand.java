package me.junick.itemBingo.commands;

import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.util.TpaManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** /tpaccept [player] — accept a pending teleport request; requester is teleported here. */
public class TpaAcceptCommand implements CommandExecutor {
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

        TpaManager.accept(p, args.length >= 1 ? args[0] : null);
        return true;
    }
}
