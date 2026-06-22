package me.junick.itemBingo.commands;

import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.util.TpaManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** /tpdeny [player] — reject a pending teleport request. */
public class TpaDenyCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }
        if (!Settings.isTpaEffective()) {
            p.sendMessage("§c/tpa 기능이 비활성화되어 있습니다.");
            return true;
        }

        TpaManager.deny(p, args.length >= 1 ? args[0] : null);
        return true;
    }
}
