package me.junick.itemBingo.commands;

import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import me.junick.itemBingo.util.GuiSync;
import me.junick.itemBingo.util.LanguageManager;
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
public class LanguageCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Messages.get(sender, "command.players-only"));
            return true;
        }

        if (args.length == 0) {
            SupportedLocale current = Messages.localeOf(p);
            p.sendMessage(Messages.get(p, "command.language.current",
                    "language", Messages.legacy(p, "language.name." + current.id())));
            p.sendMessage(Messages.get(p, "command.language.usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "english", "en", "en_us" -> apply(p, SupportedLocale.EN_US);
            case "korean", "ko", "ko_kr", "kor" -> apply(p, SupportedLocale.KO_KR);
            case "auto", "client", "reset" -> {
                LanguageManager.clearOverride(p.getUniqueId());
                SupportedLocale now = Messages.localeOf(p);
                p.sendMessage(Messages.get(p, "command.language.auto",
                        "language", Messages.legacy(p, "language.name." + now.id())));
                GuiSync.reopenFor(p);
            }
            default -> p.sendMessage(Messages.get(p, "command.language.unknown"));
        }
        return true;
    }

    private void apply(Player p, SupportedLocale loc) {
        LanguageManager.setOverride(p.getUniqueId(), loc);
        p.sendMessage(Messages.get(p, "command.language.set",
                "language", Messages.legacy(p, "language.name." + loc.id())));
        GuiSync.reopenFor(p);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String option : List.of("english", "korean", "auto")) {
                if (option.startsWith(prefix)) out.add(option);
            }
        }
        return out;
    }
}
