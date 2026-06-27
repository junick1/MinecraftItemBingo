package me.junick.itemBingo.commands;

import me.junick.itemBingo.config.PresetManager;
import me.junick.itemBingo.gui.PresetEditorGUI;
import me.junick.itemBingo.i18n.Messages;
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
 * {@code /newbingo <id> <가로> <세로>} — creates an empty preset and opens its editor so the admin
 * can fill in the board. Does not change the active game; apply it later with {@code /setbingo}.
 */
public class NewBingoCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(Messages.get(sender, "command.newbingo.usage"));
            return true;
        }

        String id = args[0];
        if (!id.matches(PresetManager.ID_PATTERN)) {
            sender.sendMessage(Messages.get(sender, "command.newbingo.invalid-id"));
            return true;
        }

        try {
            int width = Integer.parseInt(args[1]);
            int height = Integer.parseInt(args[2]);

            if (width < 1 || width > PresetManager.MAX_WIDTH || height < 1 || height > PresetManager.MAX_HEIGHT) {
                sender.sendMessage(Messages.get(sender, "command.preset.range",
                        "maxw", PresetManager.MAX_WIDTH, "maxh", PresetManager.MAX_HEIGHT));
                return true;
            }

            if (!PresetManager.create(id, width, height)) {
                sender.sendMessage(Messages.get(sender, "command.newbingo.exists", "id", id));
                return true;
            }

            sender.sendMessage(Messages.get(sender, "command.newbingo.created",
                    "id", id, "width", width, "height", height));
            if (sender instanceof Player player) {
                PresetEditorGUI.open(player, id);
            } else {
                sender.sendMessage(Messages.get(sender, "command.newbingo.edit-hint", "id", id));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Messages.get(sender, "command.invalid-number"));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            for (int i = 1; i <= PresetManager.MAX_WIDTH; i++) completions.add(String.valueOf(i));
        } else if (args.length == 3) {
            for (int i = 1; i <= PresetManager.MAX_HEIGHT; i++) completions.add(String.valueOf(i));
        }
        return completions;
    }
}
