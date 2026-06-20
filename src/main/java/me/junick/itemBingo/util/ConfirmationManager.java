package me.junick.itemBingo.util;

import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight double-confirmation guard for destructive commands.
 * The first invocation registers a pending action; a second identical
 * invocation within {@link #WINDOW_MS} confirms it.
 */
public class ConfirmationManager {
    private static final long WINDOW_MS = 10_000L;

    private record Pending(String action, long time) {}

    private static final Map<String, Pending> pending = new HashMap<>();

    /**
     * @return true if this is a confirmed (second) invocation of {@code action}
     *         within the time window; false if it was just registered as pending.
     */
    public static boolean confirm(CommandSender sender, String action) {
        String id = sender.getName();
        long now = System.currentTimeMillis();
        Pending p = pending.get(id);

        if (p != null && p.action().equals(action) && now - p.time() <= WINDOW_MS) {
            pending.remove(id);
            return true;
        }

        pending.put(id, new Pending(action, now));
        return false;
    }
}
