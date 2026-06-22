package me.junick.itemBingo.commands;

import me.junick.itemBingo.config.PresetManager;
import me.junick.itemBingo.gui.PresetEditorGUI;
import me.junick.itemBingo.gui.PresetGUI;
import me.junick.itemBingo.gui.PresetGUI.Mode;
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
 * {@code /editbingo} opens a picker to choose which preset to edit; {@code /editbingo <id>} opens
 * that preset's editor directly. Edits auto-save when the editor closes.
 */
public class EditBingoCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c편집기는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (args.length == 0) {
            if (PresetManager.getIds().isEmpty()) {
                player.sendMessage("§c저장된 프리셋이 없습니다. /newbingo <id> <가로> <세로> 로 먼저 생성하세요.");
                return true;
            }
            PresetGUI.open(player, 0, Mode.EDIT);
            return true;
        }

        String id = args[0];
        if (!PresetManager.exists(id)) {
            player.sendMessage("§c존재하지 않는 프리셋입니다: " + id);
            return true;
        }
        PresetEditorGUI.open(player, id);
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
