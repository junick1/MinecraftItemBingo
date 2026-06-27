package me.junick.itemBingo.util;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.i18n.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player chat channel state ({@link Channel#ALL} vs {@link Channel#TEAM})
 * plus the routing helpers that deliver a message to the right audience.
 *
 * <p>The chosen channel is the player's default for ordinary chat (applied by
 * {@code ChatListener}) and persists across restarts in {@code chat_modes.yml} —
 * stored separately from board/progress data so a board reset never clears it.
 * Only non-default ({@code TEAM}) entries are written, so the file stays small.
 * The {@code /ac} and {@code /tc} commands bypass the stored channel and send to
 * a specific one regardless.</p>
 */
public final class ChatManager {
    private ChatManager() {}

    public enum Channel { ALL, TEAM }

    private static final Map<UUID, Channel> channels = new HashMap<>();
    private static File file;

    /* ===================== Persistence ===================== */

    public static void load() {
        file = new File(ItemBingo.getInstance().getDataFolder(), "chat_modes.yml");
        channels.clear();
        if (!file.exists()) return;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String key : yml.getKeys(false)) {
            try {
                channels.put(UUID.fromString(key), Channel.valueOf(yml.getString(key)));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public static void save() {
        if (file == null) return;

        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, Channel> e : channels.entrySet()) {
            yml.set(e.getKey().toString(), e.getValue().name());
        }
        try {
            yml.save(file);
        } catch (IOException ex) {
            ItemBingo.getInstance().getLogger().severe("Failed to save chat_modes.yml: " + ex.getMessage());
        }
    }

    /* ===================== Channel state ===================== */

    public static Channel getChannel(Player p) {
        return channels.getOrDefault(p.getUniqueId(), Channel.ALL);
    }

    /** Sets and persists the player's default channel. ALL (the default) is not stored. */
    public static void setChannel(Player p, Channel channel) {
        if (channel == Channel.ALL) {
            channels.remove(p.getUniqueId());
        } else {
            channels.put(p.getUniqueId(), channel);
        }
        save();
    }

    /* ===================== Routing ===================== */

    /** Broadcasts {@code message} to everyone, vanilla-style ({@code <name> message}). */
    public static void sendAll(Player sender, Component message) {
        Component formatted = Component.empty()
                .append(Component.text("<"))
                .append(sender.displayName())
                .append(Component.text("> "))
                .append(message);
        Bukkit.broadcast(formatted);
    }

    /**
     * Sends {@code message} to the sender's team only, tagged with {@link #TEAM_TAG}.
     * If the sender has no team — unassigned, or team mode is off — nothing is sent
     * and they're told why (per the chosen "block with a notice" behavior).
     */
    public static void sendTeam(Player sender, Component message) {
        TeamManager tm = ItemBingo.getInstance().getTeamManager();
        int teamId = tm.effectiveTeamId(sender);
        if (teamId == TeamManager.NO_TEAM) {
            sender.sendMessage(Messages.get(sender, "chat.no-team"));
            return;
        }

        // Built per recipient so the team tag shows in each member's language.
        for (Player member : tm.getOnlinePlayersOnTeam(teamId)) {
            Component formatted = Messages.get(member, "chat.team-tag")
                    .append(sender.displayName())
                    .append(Component.text(": ", NamedTextColor.WHITE))
                    .append(message);
            member.sendMessage(formatted);
        }
    }
}
