package me.junick.itemBingo.util;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.i18n.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Teammate teleport requests (/tpa). A player requests to teleport <em>to</em> a
 * teammate; when the teammate accepts, the requester is teleported to them.
 * <p>
 * Only usable while {@link Settings#isTpaEffective()} (team mode ON + toggle on).
 * Requests expire after {@link #EXPIRY_MS}.
 */
public final class TpaManager {

    private static final long EXPIRY_MS = 60_000L;

    /** target UUID → (requester UUID → expiry epoch ms). */
    private static final Map<UUID, Map<UUID, Long>> pending = new HashMap<>();

    private TpaManager() {}

    /* ========================= Requesting ========================= */

    public static void request(Player requester, Player target) {
        if (!Settings.isTpaEffective()) {
            requester.sendMessage(Messages.get(requester, "command.tpa.disabled"));
            return;
        }
        if (requester.equals(target)) {
            requester.sendMessage(Messages.get(requester, "tpa.self"));
            return;
        }
        if (!ItemBingo.getInstance().getTeamManager().sameTeam(requester, target)) {
            requester.sendMessage(Messages.get(requester, "tpa.not-teammate"));
            return;
        }

        purgeExpired(target.getUniqueId());
        pending.computeIfAbsent(target.getUniqueId(), k -> new HashMap<>())
                .put(requester.getUniqueId(), System.currentTimeMillis() + EXPIRY_MS);

        requester.sendMessage(Messages.get(requester, "tpa.sent", "target", target.getName()));
        sendPrompt(target, requester);
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    /** Clickable [accept]/[deny] prompt that runs /tpaccept|/tpdeny <requester>. */
    private static void sendPrompt(Player target, Player requester) {
        Component accept = Messages.get(target, "tpa.accept-button")
                .hoverEvent(HoverEvent.showText(Messages.get(target, "tpa.accept-hover", "requester", requester.getName())))
                .clickEvent(ClickEvent.runCommand("/tpaccept " + requester.getName()));
        Component deny = Messages.get(target, "tpa.deny-button")
                .hoverEvent(HoverEvent.showText(Messages.get(target, "tpa.deny-hover")))
                .clickEvent(ClickEvent.runCommand("/tpdeny " + requester.getName()));

        target.sendMessage(Messages.get(target, "tpa.request-received", "requester", requester.getName())
                .append(accept).append(Component.text(" ")).append(deny));
    }

    /* ========================= Accepting / Denying ========================= */

    /**
     * @param requesterName the requester to accept, or {@code null} to accept the
     *                      sole pending request (errors if there are 0 or many).
     */
    public static void accept(Player target, String requesterName) {
        Player requester = resolve(target, requesterName, true);
        if (requester == null) return;

        pending.getOrDefault(target.getUniqueId(), Map.of()).remove(requester.getUniqueId());

        if (!ItemBingo.getInstance().getTeamManager().sameTeam(requester, target)) {
            target.sendMessage(Messages.get(target, "tpa.not-same-team"));
            return;
        }

        requester.teleport(target.getLocation());
        requester.playSound(requester.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        requester.sendMessage(Messages.get(requester, "tpa.teleported", "target", target.getName()));
        target.sendMessage(Messages.get(target, "tpa.accepted", "requester", requester.getName()));
    }

    public static void deny(Player target, String requesterName) {
        Player requester = resolve(target, requesterName, false);
        if (requester == null) return;

        pending.getOrDefault(target.getUniqueId(), Map.of()).remove(requester.getUniqueId());
        target.sendMessage(Messages.get(target, "tpa.denied-by-you", "requester", requester.getName()));
        requester.sendMessage(Messages.get(requester, "tpa.denied", "target", target.getName()));
    }

    /**
     * Resolves which requester an accept/deny refers to, handling the empty /
     * unknown / ambiguous cases with a message to {@code target}.
     */
    private static Player resolve(Player target, String requesterName, boolean teleporting) {
        purgeExpired(target.getUniqueId());
        Map<UUID, Long> reqs = pending.get(target.getUniqueId());
        if (reqs == null || reqs.isEmpty()) {
            target.sendMessage(Messages.get(target, "tpa.none-pending"));
            return null;
        }

        if (requesterName == null) {
            if (reqs.size() > 1) {
                target.sendMessage(Messages.get(target, "tpa.ambiguous", "sub", teleporting ? "accept" : "deny"));
                return null;
            }
            UUID only = reqs.keySet().iterator().next();
            Player p = Bukkit.getPlayer(only);
            if (p == null) {
                reqs.remove(only);
                target.sendMessage(Messages.get(target, "tpa.requester-offline"));
            }
            return p;
        }

        Player requester = Bukkit.getPlayerExact(requesterName);
        if (requester == null || !reqs.containsKey(requester.getUniqueId())) {
            target.sendMessage(Messages.get(target, "tpa.not-found", "player", requesterName));
            return null;
        }
        return requester;
    }

    private static void purgeExpired(UUID target) {
        Map<UUID, Long> reqs = pending.get(target);
        if (reqs == null) return;
        long now = System.currentTimeMillis();
        reqs.values().removeIf(expiry -> expiry < now);
        if (reqs.isEmpty()) pending.remove(target);
    }
}
