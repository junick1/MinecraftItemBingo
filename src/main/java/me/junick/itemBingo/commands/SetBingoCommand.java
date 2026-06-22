package me.junick.itemBingo.commands;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.PresetManager;
import me.junick.itemBingo.config.PresetManager.PresetInfo;
import me.junick.itemBingo.gui.PresetGUI;
import me.junick.itemBingo.gui.PresetGUI.Mode;
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
                sender.sendMessage("§cGUI는 플레이어만 열 수 있습니다. /setbingo <id> 를 사용하세요.");
                return true;
            }
            if (PresetManager.getIds().isEmpty()) {
                sender.sendMessage("§c저장된 프리셋이 없습니다. /newbingo <id> <가로> <세로> 로 먼저 생성하세요.");
                return true;
            }
            PresetGUI.open(player, 0, Mode.APPLY);
            return true;
        }

        String id = args[0];
        PresetInfo info = PresetManager.getInfo(id);
        if (info == null) {
            sender.sendMessage("§c존재하지 않는 프리셋입니다: " + id);
            return true;
        }
        if (!info.isComplete()) {
            sender.sendMessage("§c미완성 프리셋입니다 (" + info.filled() + "/" + info.slotCount()
                    + "). §7/editbingo " + id + " §c에서 먼저 완료하세요.");
            return true;
        }

        if (!ConfirmationManager.confirm(sender, "setbingo:" + id)) {
            sender.sendMessage("§c§l[경고] §f프리셋을 적용하면 모든 플레이어와 팀의 진행 데이터가 초기화됩니다.");
            sender.sendMessage("§e계속하려면 10초 안에 §f/setbingo " + id + "§e을(를) 다시 입력하세요.");
            return true;
        }

        BingoBoard board = PresetManager.load(id);
        ItemBingo.applyNewBoard(board);
        sender.sendMessage("§a프리셋 '" + id + "' (" + board.getWidth() + "x" + board.getHeight() + ")을(를) 적용했습니다!");
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
