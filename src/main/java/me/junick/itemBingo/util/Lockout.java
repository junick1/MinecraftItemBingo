package me.junick.itemBingo.util;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.model.PlayerBingoProgress;
import me.junick.itemBingo.model.TeamBingoProgress;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Lockout mode logic: a cell, once submitted by a team (or a solo player), is
 * "claimed" and locked out for everyone else — no other team can ever submit it.
 * Whoever submits first wins the cell; simultaneous submissions are effectively
 * impossible in practice and resolve to whichever was processed first.
 *
 * <p>There is no separate lock store — a lock is simply another team's (or solo
 * player's) submission, so we derive it from the existing per-team / per-player
 * progress that is already persisted. Mirrors {@link FogOfWar} in being a pure,
 * stateless helper.
 */
public final class Lockout {
    private Lockout() {}

    /**
     * The board cells that have been claimed by someone OTHER than {@code viewer}'s
     * team (or, when not in team mode, another solo player). These are locked out:
     * {@code viewer} can never submit them. Empty when not in Lockout mode.
     *
     * <p>Cells the viewer's own team has submitted are excluded — those render as
     * normal submitted cells, not locks.
     */
    public static Set<Integer> lockedSlots(Player viewer) {
        if (!Settings.isLockoutMode()) return Collections.emptySet();

        Set<Integer> locked = new HashSet<>();
        TeamManager tm = ItemBingo.getInstance().getTeamManager();
        int myTeam = tm.effectiveTeamId(viewer);

        if (myTeam != TeamManager.NO_TEAM) {
            for (Map.Entry<Integer, TeamBingoProgress> e : TeamDataManager.getAllData().entrySet()) {
                if (e.getKey() == myTeam) continue;
                locked.addAll(e.getValue().getSubmittedSlots());
            }
        } else {
            UUID me = viewer.getUniqueId();
            for (Map.Entry<UUID, PlayerBingoProgress> e : PlayerDataManager.getAllData().entrySet()) {
                if (e.getKey().equals(me)) continue;
                locked.addAll(e.getValue().getSubmittedSlots());
            }
        }
        return locked;
    }
}
