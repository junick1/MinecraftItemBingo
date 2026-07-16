package me.junick.itemBingo.util;

import me.junick.itemBingo.interfaces.access.BingoProgressAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * How one board cell appears to one viewer. The priority order lives here and
 * ONLY here — the chest GUI ({@code BingoGUI.cellIcon}) and the companion-mod
 * serializer ({@code BoardViewBuilder}) both resolve through this, so the two
 * surfaces can never drift apart.
 */
public enum BoardCellView {
    VISIBLE, HIDDEN, LOCKED, SUBMITTED;

    /**
     * @param revealed the fog-revealed set, or {@code null} when fog doesn't
     *                 apply (not fog mode, or a bare preview)
     * @param locked   lockout-claimed cells (empty outside Lockout mode)
     */
    public static BoardCellView of(int index, BingoProgressAccess progress,
                                   @Nullable Set<Integer> revealed, Set<Integer> locked) {
        if (progress.isSubmitted(index)) return SUBMITTED;
        if (locked.contains(index)) return LOCKED;
        if (revealed != null && !revealed.contains(index)) return HIDDEN;
        return VISIBLE;
    }
}
