package me.junick.itemBingo.admin;

import me.junick.itemBingo.ItemBingo;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminCommand implements CommandExecutor {
    private final ItemBingo plugin;

    public AdminCommand(ItemBingo plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (!player.isOp() || !player.hasPermission("itembingo.admin")) {
            player.sendMessage("§c권한이 없습니다.");
            return true;
        }

        AdminGUI.openMain(plugin, player);
        return true;
    }
}
