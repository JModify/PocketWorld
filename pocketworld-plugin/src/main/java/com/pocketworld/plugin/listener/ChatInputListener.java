package com.pocketworld.plugin.listener;

import com.pocketworld.plugin.PocketWorldPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Bridges chat messages into {@link com.pocketworld.plugin.util.ChatInputRegistry} pending prompts.
 * Uses the plain Bukkit/Spigot {@link AsyncPlayerChatEvent} rather than Paper's own
 * {@code io.papermc.paper.event.player.AsyncChatEvent} so the same listener works on both platforms -
 * {@link #onPlayerChat} runs at {@link org.bukkit.event.EventPriority#LOWEST} specifically so this
 * plugin's cancellation happens before any other plugin's chat formatter/renderer sees the event, which
 * is what actually determines whether cancellation suppresses the message on Paper as well as Spigot.
 */
public class ChatInputListener implements Listener {

    private final PocketWorldPlugin plugin;

    public ChatInputListener(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.getChatInputRegistry().handle(player.getUniqueId(), event.getMessage())) {
            event.setCancelled(true);
        }
    }
}
