package com.pocketworld.plugin.util;

import com.pocketworld.plugin.data.config.MessageFile;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class MessageReader {

    private final MessageFile messageFile;

    public MessageReader(MessageFile messageFile) {
        this.messageFile = messageFile;
    }

    public String read(String path) {
        ConfigurationSection section = messageFile.getYaml().getConfigurationSection("messages." + path);
        if (section == null) {
            return null;
        }

        String type = section.getString("type", null);
        String message = section.getString("message", null);
        if (type == null || type.equalsIgnoreCase("raw")) {
            return ColorFormat.format(message);
        }

        String prefix = messageFile.getYaml().getString("prefixes." + type, null);
        if (prefix == null) {
            return ColorFormat.format(message);
        }

        return ColorFormat.format(prefix + message);
    }

    public String read(String path, String... placeholders) {
        return replacePlaceholders(read(path), placeholders);
    }

    public void send(String path, CommandSender commandSender) {
        commandSender.sendMessage(read(path));
    }

    /** Sends via the action bar rather than chat - for transient status that would otherwise spam
     *  chat if repeated (e.g. a live-updating queue position). Uses the bungeecord-chat API
     *  (bundled in spigot-api itself, not a Paper-only Adventure type) so this works identically on
     *  Spigot and Paper. */
    public void sendActionBar(String path, Player player, String... placeholders) {
        String formatted = read(path, placeholders);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(formatted));
    }

    /**
     * Sends the message with placeholders substituted in. Each placeholder is given in
     * {@code PLACEHOLDER:REPLACEMENT} form, e.g. {@code "{TIME}:1500"} - only the first colon
     * separates the placeholder from its replacement, so the replacement text itself may contain
     * colons (a formatted time, a world name, etc).
     */
    public void send(String path, CommandSender commandSender, String... placeholders) {
        commandSender.sendMessage(read(path, placeholders));
    }

    private String replacePlaceholders(String message, String... placeholders) {
        for (String raw : placeholders) {
            String[] parts = raw.split(":", 2);
            if (parts.length != 2) {
                continue;
            }

            message = message.replace(parts[0], parts[1]);
        }
        return message;
    }
}
