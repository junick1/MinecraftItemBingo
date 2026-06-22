package me.junick.itemBingo.events;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.junick.itemBingo.util.ChatManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Routes ordinary chat according to each player's chosen channel
 * ({@link ChatManager}). Players in {@link ChatManager.Channel#ALL} are left to
 * vanilla broadcast (so all chat looks untagged); players in
 * {@link ChatManager.Channel#TEAM} have their message redirected to their team.
 */
public class ChatListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        if (ChatManager.getChannel(p) != ChatManager.Channel.TEAM) return;

        // TEAM channel: don't let it hit the vanilla all-chat broadcast.
        e.setCancelled(true);
        ChatManager.sendTeam(p, e.message());
    }
}
