package me.junick.itemBingo.util;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
            requester.sendMessage(Component.text("/tpa 기능이 비활성화되어 있습니다.", NamedTextColor.RED));
            return;
        }
        if (requester.equals(target)) {
            requester.sendMessage(Component.text("자기 자신에게는 요청할 수 없습니다.", NamedTextColor.RED));
            return;
        }
        if (!ItemBingo.getInstance().getTeamManager().sameTeam(requester, target)) {
            requester.sendMessage(Component.text("같은 팀원에게만 요청할 수 있습니다.", NamedTextColor.RED));
            return;
        }

        purgeExpired(target.getUniqueId());
        pending.computeIfAbsent(target.getUniqueId(), k -> new HashMap<>())
                .put(requester.getUniqueId(), System.currentTimeMillis() + EXPIRY_MS);

        requester.sendMessage(Component.text(target.getName() + " 님에게 텔레포트 요청을 보냈습니다. (60초 내 수락 필요)", NamedTextColor.GREEN));
        sendPrompt(target, requester);
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    /** Clickable [수락]/[거절] prompt that runs /tpaccept|/tpdeny <requester>. */
    private static void sendPrompt(Player target, Player requester) {
        Component accept = Component.text("[수락]", NamedTextColor.GREEN, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("클릭하면 " + requester.getName() + " 님을 받아줍니다")))
                .clickEvent(ClickEvent.runCommand("/tpaccept " + requester.getName()));
        Component deny = Component.text("[거절]", NamedTextColor.RED, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("클릭하면 요청을 거절합니다")))
                .clickEvent(ClickEvent.runCommand("/tpdeny " + requester.getName()));

        target.sendMessage(Component.text(requester.getName() + " 님이 당신에게 텔레포트를 요청했습니다. ", NamedTextColor.AQUA)
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
            target.sendMessage(Component.text("더 이상 같은 팀이 아니라 텔레포트할 수 없습니다.", NamedTextColor.RED));
            return;
        }

        requester.teleport(target.getLocation());
        requester.playSound(requester.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        requester.sendMessage(Component.text(target.getName() + " 님에게 텔레포트했습니다.", NamedTextColor.GREEN));
        target.sendMessage(Component.text(requester.getName() + " 님의 텔레포트 요청을 수락했습니다.", NamedTextColor.GREEN));
    }

    public static void deny(Player target, String requesterName) {
        Player requester = resolve(target, requesterName, false);
        if (requester == null) return;

        pending.getOrDefault(target.getUniqueId(), Map.of()).remove(requester.getUniqueId());
        target.sendMessage(Component.text(requester.getName() + " 님의 요청을 거절했습니다.", NamedTextColor.YELLOW));
        requester.sendMessage(Component.text(target.getName() + " 님이 텔레포트 요청을 거절했습니다.", NamedTextColor.RED));
    }

    /**
     * Resolves which requester an accept/deny refers to, handling the empty /
     * unknown / ambiguous cases with a message to {@code target}.
     */
    private static Player resolve(Player target, String requesterName, boolean teleporting) {
        purgeExpired(target.getUniqueId());
        Map<UUID, Long> reqs = pending.get(target.getUniqueId());
        if (reqs == null || reqs.isEmpty()) {
            target.sendMessage(Component.text("대기 중인 텔레포트 요청이 없습니다.", NamedTextColor.RED));
            return null;
        }

        if (requesterName == null) {
            if (reqs.size() > 1) {
                target.sendMessage(Component.text("여러 요청이 있습니다. /tp" + (teleporting ? "accept" : "deny") + " <플레이어> 로 지정하세요.", NamedTextColor.RED));
                return null;
            }
            UUID only = reqs.keySet().iterator().next();
            Player p = Bukkit.getPlayer(only);
            if (p == null) {
                reqs.remove(only);
                target.sendMessage(Component.text("요청자가 오프라인입니다.", NamedTextColor.RED));
            }
            return p;
        }

        Player requester = Bukkit.getPlayerExact(requesterName);
        if (requester == null || !reqs.containsKey(requester.getUniqueId())) {
            target.sendMessage(Component.text(requesterName + " 님의 요청을 찾을 수 없습니다.", NamedTextColor.RED));
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
