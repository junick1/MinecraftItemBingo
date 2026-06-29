package me.junick.itemBingo.commands;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.PresetManager;
import me.junick.itemBingo.config.PresetManager.PresetInfo;
import me.junick.itemBingo.gui.PresetGUI;
import me.junick.itemBingo.gui.PresetGUI.Mode;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.model.BingoBoard;
import me.junick.itemBingo.util.ConfirmationManager;
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
 * {@code /setbingo} opens the preset browser GUI; {@code /setbingo <id>} applies a preset directly.
 *
 * <p>Applying a preset wipes all player/team progress (same as {@code /rollbingo}), so the direct
 * form is double-confirmed via {@link ConfirmationManager}.</p>
 */
public class SetBingoCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Messages.get(sender, "command.setbingo.gui-players-only"));
                return true;
            }
            if (PresetManager.getIds().isEmpty()) {
                sender.sendMessage(Messages.get(sender, "command.setbingo.no-presets"));
                return true;
            }
            PresetGUI.open(player, 0, Mode.APPLY);
            return true;
        }

        String id = args[0];
        PresetInfo info = PresetManager.getInfo(id);
        if (info == null) {
            sender.sendMessage(Messages.get(sender, "command.setbingo.not-found", "id", id));
            return true;
        }
        if (!info.isComplete()) {
            sender.sendMessage(Messages.get(sender, "command.setbingo.incomplete",
                    "filled", info.filled(), "slots", info.slotCount(), "id", id));
            return true;
        }

        if (!ConfirmationManager.confirm(sender, "setbingo:" + id)) {
            sender.sendMessage(Messages.get(sender, "command.setbingo.warn"));
            sender.sendMessage(Messages.get(sender, "command.setbingo.confirm", "id", id));
            return true;
        }

        BingoBoard board = PresetManager.load(id);
        ItemBingo.applyNewBoard(board);
        sender.sendMessage(Messages.get(sender, "command.setbingo.applied",
                "id", id, "width", board.getWidth(), "height", board.getHeight()));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (String id : PresetManager.getIds()) {
                if (id.toLowerCase().startsWith(prefix)) completions.add(id);
            }
            return completions;
        }
        return new ArrayList<>();
    }
}
