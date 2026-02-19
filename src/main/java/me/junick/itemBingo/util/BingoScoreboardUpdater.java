package me.junick.itemBingo.util;

import me.junick.itemBingo.ItemBingo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BingoScoreboardUpdater extends BukkitRunnable {
    @Override
    public void run() {
        if (ItemBingo.currentBingo == null) return;

        BingoScoreboard.updateAll();
    }
}
