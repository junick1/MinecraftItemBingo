package me.junick.itemBingo.commands;

import me.junick.itemBingo.config.BundleManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class SetBundleCommand implements CommandExecutor, Listener {

    public SetBundleCommand() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            BundleManager.openStorage(player);
            return true;
        }
        return false;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(BundleManager.TITLE)) {
            BundleManager.saveStorage(event.getInventory());
        }
    }
}