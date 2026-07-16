package me.junick.itemBingo.util;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import me.junick.itemBingo.records.ranking.RankingEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TimerManager {
    /**
     * Where the current board is in its life: rolled but not yet started,
     * mid-game, or over. Any stop — natural finish or manual /timer stop —
     * counts as ENDED; a new board resets to NOT_STARTED (unless a game is
     * actively running across the swap).
     */
    public enum GameStage { NOT_STARTED, RUNNING, ENDED }

    private static GameStage stage = GameStage.NOT_STARTED;
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
        config.set("stage", stage.name());
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
        try {
            stage = GameStage.valueOf(config.getString("stage", ""));
        } catch (IllegalArgumentException e) {
            // Pre-stage save file: all we know is whether a game is running.
            stage = running ? GameStage.RUNNING : GameStage.NOT_STARTED;
        }
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
                    finish();
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
                boolean swapBar = Settings.isPositionSwapMode() && Settings.isSwapTimer();
                String timeStr = formatTime(remainingSeconds);
                String swapStr = formatTime(swapRemaining);
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Component actionBar = swapBar
                            ? Messages.get(player, "timer.actionbar-swap", "time", timeStr, "swap", swapStr)
                            : Messages.get(player, "timer.actionbar", "time", timeStr);
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
        stage = GameStage.RUNNING;
        setSwap();
        resumeTask();
        // Stage gates what the companion mod may export — keep clients current.
        me.junick.itemBingo.network.ModSync.broadcastBoard();
    }

    public static void stop() {
        running = false;
        paused = false;
        remainingSeconds = 0;
        elapsedSeconds = 0;
        stage = GameStage.ENDED;

        if (task != null) {
            task.cancel();
            task = null;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(Messages.get(player, "timer.ended"));
        }
        me.junick.itemBingo.network.ModSync.broadcastBoard();
    }

    /**
     * A new board was installed: back to pre-game, unless a game is actively
     * running across the board swap (then it simply continues).
     */
    public static void onNewBoard() {
        if (!running) {
            stage = GameStage.NOT_STARTED;
        }
    }

    public static GameStage getStage() {
        return stage;
    }

    /**
     * Natural end of the game (the countdown reached zero): announce the winner
     * with a title, then run the normal {@link #stop()} cleanup. Distinct from a
     * bare {@code stop()} (manual /timer stop or a reset before a new start),
     * which ends the timer silently without a winner screen.
     */
    public static void finish() {
        announceWinner();
        stop();
        // The timer is now inactive, so a hidden leaderboard becomes visible —
        // refresh the sidebar immediately rather than waiting for the next tick.
        BingoScoreboard.updateAll();
    }

    /** Shows every player a title with the top-ranked player/team, score and penalty. */
    private static void announceWinner() {
        List<RankingEntry> top = RankingProviders.current().getRankings(1);
        boolean hasWinner = !top.isEmpty() && top.get(0).score() > 0;
        RankingEntry winner = hasWinner ? top.get(0) : null;

        Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(6), Duration.ofSeconds(1));
        // Built per player so each sees the result in their language.
        for (Player player : Bukkit.getOnlinePlayers()) {
            SupportedLocale loc = Messages.localeOf(player);
            Component mainTitle;
            Component subtitle;
            if (!hasWinner) {
                mainTitle = Messages.get(loc, "timer.game-over");
                subtitle = Messages.get(loc, "timer.no-submitter");
            } else {
                mainTitle = Messages.get(loc, "timer.winner", "name", winner.displayName());
                subtitle = Messages.get(loc, "timer.winner-sub",
                        "score", winner.score(),
                        "penalty", RankingFormat.penalty(winner.penaltySeconds(), loc));
            }
            player.showTitle(Title.title(mainTitle, subtitle, times));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
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
                target.sendMessage(Messages.get(target, "timer.swapped"));

                // 이동 시 소리 효과!
                target.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }

            Bukkit.getLogger().info("[ItemBingo] All player positions swapped successfully.");
        }
    }
}
