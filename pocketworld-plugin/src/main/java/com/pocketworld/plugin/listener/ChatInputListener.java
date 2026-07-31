package com.pocketworld.plugin.listener;

import com.pocketworld.plugin.PocketWorldPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/** Bridges chat messages into {@link com.pocketworld.plugin.util.ChatInputRegistry} pending prompts. */
public class ChatInputListener implements Listener {

    private final PocketWorldPlugin plugin;

    public ChatInputListener(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.getChatInputRegistry().handle(player.getUniqueId(), event.getMessage())) {
            event.setCancelled(true);
        }
    }
}
