package me.junick.itemBingo.util;

import me.junick.itemBingo.ItemBingo;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;

public class TimerManager {
    private static boolean running = false;
    private static boolean paused = false;
    private static int maxSeconds = 1;
    private static int remainingSeconds = 0;
    private static int elapsedSeconds = 0;
    private static BukkitRunnable task;

    private static final File file = new File(ItemBingo.getInstance().getDataFolder(), "timer.yml");

    public static void saveState() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("running", running);
        config.set("paused", paused);
        config.set("remaining", remainingSeconds);
        config.set("elapsed", elapsedSeconds);
        config.set("max", maxSeconds);

        try {
            config.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().severe("[ItemBingo] Failed to save timer state: " + e.getMessage());
        }
    }

    public static void loadState() {
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        running = config.getBoolean("running", false);
        paused = config.getBoolean("paused", false);
        remainingSeconds = config.getInt("remaining", 0);
        elapsedSeconds = config.getInt("elapsed", 0);
        maxSeconds = config.getInt("max", 0);

        if (running) {
            resumeTask();
        }
    }

    private static void resumeTask() {
        if (task != null) task.cancel();

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!running || paused) return;

                if (remainingSeconds <= 0) {
                    stop();
                    return;
                }

                remainingSeconds--;
                elapsedSeconds++;

                Component actionBar = Component.text("§a남은 시간: §e" + formatTime(remainingSeconds));
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendActionBar(actionBar);
                }
            }
        };
        task.runTaskTimer(ItemBingo.getInstance(), 0, 20);
    }

    public static void start(int seconds) {
        stop();
        remainingSeconds = seconds;
        maxSeconds = seconds;
        elapsedSeconds = 0;
        paused = false;
        running = true;

        resumeTask();
    }

    public static void stop() {
        running = false;
        paused = false;
        remainingSeconds = 0;
        elapsedSeconds = 0;

        if (task != null) {
            task.cancel();
            task = null;
        }

        Component actionBar = Component.text("§c§l타이머 종료");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(actionBar);
        }
    }

    public static boolean togglePause() {
        if (!running) return false;
        paused = !paused;
        return paused;
    }

    public static boolean isRunning() {
        return running;
    }

    public static boolean isPaused() {
        return paused;
    }

    public static int getRemainingSeconds() {
        return remainingSeconds;
    }

    public static int getElapsedSeconds() {
        return elapsedSeconds;
    }

    public static int getLastMaxSeconds() {
        return maxSeconds;
    }

    private static String formatTime(int totalSeconds) {
        int mins = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }
}
