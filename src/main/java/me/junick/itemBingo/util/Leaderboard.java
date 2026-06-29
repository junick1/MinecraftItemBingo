package me.junick.itemBingo.util;

import me.junick.itemBingo.config.Settings;

/**
 * Visibility rule for the ranking sidebar and {@code /rank}.
 *
 * <p>When the admin "hide leaderboard" option is on, rankings are concealed
 * <em>during play</em> and only revealed once the game ends. "During play" means
 * the game timer is actively running — a stopped/expired timer reveals the board
 * again so everyone can see the final standings. Single source of truth so the
 * sidebar ({@link BingoScoreboard}) and {@code /rank}
 * ({@link RankMessageGenerator}) stay in agreement.
 */
public final class Leaderboard {
    private Leaderboard() {}

    /** True when rankings should be concealed right now. */
    public static boolean isHidden() {
        return Settings.isHideLeaderboard() && TimerManager.isRunning();
    }
}
