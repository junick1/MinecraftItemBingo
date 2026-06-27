package me.junick.itemBingo.commands;

import me.junick.itemBingo.gui.MenuGUI;
import me.junick.itemBingo.i18n.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MenuCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Messages.get(sender, "command.players-only"));
            return true;
        }

        MenuGUI.openMain(p);
        return true;
    }
}
