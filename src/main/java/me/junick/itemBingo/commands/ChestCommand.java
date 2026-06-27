package me.junick.itemBingo.commands;

import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.util.ChestManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ChestCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Messages.get(sender, "command.players-only"));
            return true;
        }

        ChestManager.open(p);
        return true;
    }
}
