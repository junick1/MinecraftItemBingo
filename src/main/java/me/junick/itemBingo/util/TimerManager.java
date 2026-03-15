package me.junick.itemBingo.util;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TimerManager {
    private static boolean running = false;
    private static boolean paused = false;
    private static int maxSeconds = 1;
    private static int remainingSeconds = 0;
    private static int elapsedSeconds = 0;
    private static int swapRemaining = 0;
    private static final int SWAP_MIN = 6;
    private static final int SWAP_MAX = 120;
    private static final Random random = new Random();
    private static BukkitRunnable task;


    private static final File file = new File(ItemBingo.getInstance().getDataFolder(), "timer.yml");

    public static void saveState() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("running", running);
        config.set("paused", paused);
        config.set("remaining", remainingSeconds);
        config.set("elapsed", elapsedSeconds);
        config.set("max", maxSeconds);
        config.set("swapRemaining", swapRemaining);

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
        swapRemaining = config.getInt("swapRemaining", 0);

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
                swapRemaining--;

                if (Settings.isPositionSwapMode() && Settings.isSwapAlert() && swapRemaining >= 1 && swapRemaining <= 3) {
                    Bukkit.getOnlinePlayers().forEach(pl -> pl.sendMessage(Component.text(String.valueOf(swapRemaining), NamedTextColor.RED)));
                }
                if (swapRemaining <= 0) {
                    doSwap();
                    setSwap();
                }
                Component actionBar = Component.text("남은 시간: ", NamedTextColor.GREEN).append(
                        Component.text(formatTime(remainingSeconds), NamedTextColor.YELLOW)
                );
                if (Settings.isPositionSwapMode() && Settings.isSwapTimer()) {
                    actionBar = Component.text("").append(
                            Component.text("남은 시간: ", NamedTextColor.YELLOW),
                            Component.text(formatTime(remainingSeconds), NamedTextColor.YELLOW),
                            Component.text(" | ", NamedTextColor.WHITE),
                            Component.text("swap 시간: ", NamedTextColor.RED),
                            Component.text(formatTime(swapRemaining), NamedTextColor.RED)
                    );
                }

                // ✨
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
        setSwap();
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

    private static void setSwap() {
        swapRemaining = random.nextInt(SWAP_MIN, SWAP_MAX);
    }

    private static void doSwap() {
        if (Settings.isPositionSwapMode()) {
            // 1. 현재 접속 중인 플레이어 목록을 가져와요!
            List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

            // 2. 2명 이상이어야 서로 바꿀 수 있어요.
            if (players.size() < 2) {
                return;
            }

            // 3. 현재 위치들을 모두 복사해서 저장해둡니다.
            List<Location> locations = new ArrayList<>();
            List<Integer> perm = new ArrayList<>();
            List<Integer> shuffled = new ArrayList<>();
            for (Player p : players) {
                locations.add(p.getLocation());
            }
            for (int i = 0; i < players.size(); i++) perm.add(i);

            while (true) {
                shuffled = new ArrayList<>(perm);
                Collections.shuffle(shuffled);
                boolean flag = true;
                for (int i = 0; i < players.size(); i++) {
                    if (shuffled.get(i).intValue() == perm.get(i).intValue()) {
                        flag = false;
                        break;
                    }
                }
                if (flag) break;
            }

            // 4. ⭐ 아리스의 특제 셔플!
            // 리스트를 무작위로 섞습니다.


            // 5. 자기 위치 방지 로직:
            // 섞인 결과가 우연히 원래 위치와 같을 수 있으므로,
            // 리스트를 한 칸씩 강제로 밀어서(Rotate) 겹침을 방지해요!
            Collections.rotate(locations, 1);

            // 6. 플레이어들에게 새로운 위치를 부여합니다.
            for (int i = 0; i < players.size(); i++) {
                Player target = players.get(i);
                Location newLoc = locations.get(i);

                target.teleport(newLoc);
                target.sendMessage("§b§l[!] §f누군가와 위치가 바뀌었습니다! 슈슉-!");

                // 이동 시 소리 효과!
                target.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }

            Bukkit.getLogger().info("✨ 모든 플레이어의 위치가 성공적으로 교체되었습니다.");
        }
    }
}
