package me.junick.itemBingo.commands;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.gui.BingoGUI;
import me.junick.itemBingo.gui.SummaryGUI;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.util.TimerManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SummaryCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Messages.get(sender, "command.players-only"));
            return true;
        }

        // /summary board | /summary b — open the read-only original-board preview.
        if (args.length >= 1) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("board") || sub.equals("b")) {
                openBoardPreview(p);
                return true;
            }
        }

        SummaryGUI.tryOpen(p);
        return true;
    }

    /** Gated like {@link SummaryGUI#tryOpen} — it's a post-game recap of the board. */
    private void openBoardPreview(Player p) {
        if (ItemBingo.currentBingo == null) {
            p.sendMessage(Messages.get(p, "gui.summary.no-game"));
            return;
        }
        if (TimerManager.isRunning()) {
            p.sendMessage(Messages.get(p, "gui.summary.in-progress"));
            return;
        }
        BingoGUI.openPreview(p);
    }

    /** The {@code board}/{@code b} arguments are intentionally not suggested. */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        return Collections.emptyList();
    }
}
