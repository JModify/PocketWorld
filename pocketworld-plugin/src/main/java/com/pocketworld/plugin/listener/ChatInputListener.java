package com.pocketworld.plugin.listener;

import com.pocketworld.plugin.PocketWorldPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Bridges chat messages into {@link com.pocketworld.plugin.util.ChatInputRegistry} pending prompts.
 * Uses Paper's {@link AsyncChatEvent} rather than the deprecated, legacy-compatibility
 * {@code org.bukkit.event.player.AsyncPlayerChatEvent} - this plugin only ever targets Paper (never
 * vanilla Bukkit/Spigot), and cancelling the legacy event isn't reliably guaranteed to suppress the
 * message Paper's own chat pipeline actually renders in every configuration.
 */
public class ChatInputListener implements Listener {

    private final PocketWorldPlugin plugin;

    public ChatInputListener(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (plugin.getChatInputRegistry().handle(player.getUniqueId(), message)) {
            event.setCancelled(true);
        }
    }
}
