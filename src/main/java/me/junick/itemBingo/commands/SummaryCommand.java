package me.junick.itemBingo.commands;

import me.junick.itemBingo.gui.SummaryGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SummaryCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Component.text("플레이어만 사용할 수 있습니다.", NamedTextColor.RED));
            return true;
        }
        SummaryGUI.tryOpen(p);
        return true;
    }
}
