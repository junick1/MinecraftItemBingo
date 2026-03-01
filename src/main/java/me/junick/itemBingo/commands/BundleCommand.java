package me.junick.itemBingo.commands;

import me.junick.itemBingo.config.BundleManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BundleCommand implements CommandExecutor {

    public BundleCommand() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            BundleManager.getBundle(player);
            return true;
        }
        return false;
    }

}