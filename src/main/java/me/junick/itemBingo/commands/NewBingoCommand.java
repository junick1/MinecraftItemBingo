package me.junick.itemBingo.commands;

import me.junick.itemBingo.config.PresetManager;
import me.junick.itemBingo.gui.PresetEditorGUI;
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
            sender.sendMessage("§c사용법: /newbingo <id> <가로> <세로>");
            return true;
        }

        String id = args[0];
        if (!id.matches(PresetManager.ID_PATTERN)) {
            sender.sendMessage("§cid에는 영문, 숫자, _, - 만 사용할 수 있습니다.");
            return true;
        }

        try {
            int width = Integer.parseInt(args[1]);
            int height = Integer.parseInt(args[2]);

            if (width < 1 || width > PresetManager.MAX_WIDTH || height < 1 || height > PresetManager.MAX_HEIGHT) {
                sender.sendMessage("§c가로는 1~" + PresetManager.MAX_WIDTH + ", 세로는 1~" + PresetManager.MAX_HEIGHT + " 사이여야 합니다.");
                return true;
            }

            if (!PresetManager.create(id, width, height)) {
                sender.sendMessage("§c이미 존재하는 프리셋 id입니다: " + id);
                return true;
            }

            sender.sendMessage("§a프리셋 '" + id + "' (" + width + "x" + height + ")이(가) 생성되었습니다!");
            if (sender instanceof Player player) {
                PresetEditorGUI.open(player, id);
            } else {
                sender.sendMessage("§7게임 내에서 §f/editbingo " + id + " §7로 편집하세요.");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§c올바른 정수를 입력하세요.");
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
