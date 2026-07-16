package me.junick.itemBingo.commands;

import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import me.junick.itemBingo.util.GuiSync;
import me.junick.itemBingo.util.LanguageManager;
import me.junick.itemBingo.util.PasswordManager;
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
 * {@code /language <english|korean|auto>} — sets the player's display language.
 * {@code english}/{@code korean} store an explicit override ({@link LanguageManager});
 * {@code auto} clears it so the player follows their Minecraft client locale. The
 * confirmation is shown in the newly chosen language, and any open GUI is reopened
 * so the change is visible immediately.
 */
public class PasswordCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Messages.get(sender, "command.players-only"));
            return true;
        }

        if (args.length == 0) {
            p.sendMessage(Messages.get(p, "command.password.reset"));
            PasswordManager.clearPassword(p.getUniqueId());
            return true;
        }

        if (args.length > 1) {
            p.sendMessage(Messages.get(p, "command.password.space"));
            return true;
        }

        String password = args[0];
        p.sendMessage(Messages.get(p, "command.password.ok"));
        PasswordManager.setPassword(p.getUniqueId(), password);
        return true;
    }
}
