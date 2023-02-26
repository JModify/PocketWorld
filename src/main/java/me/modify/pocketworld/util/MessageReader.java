package me.modify.pocketworld.util;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.config.MessageFile;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

public class MessageReader {

    private PocketWorldPlugin plugin;
    private final MessageFile messageFile;
    public MessageReader(PocketWorldPlugin plugin, MessageFile messageFile) {
        this.plugin = plugin;
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
        String message = read(path);
        message = replacePlaceholders(message, placeholders);
        return message;
    }

    public void send(String path, CommandSender commandSender) {
        String message = read(path);
        commandSender.sendMessage(message);
    }

    /**
     * Sends the desired message with placeholders.
     * Placeholders should be in the string format PLACEHOLDER:REPLACEMENT.
     * For example: {USAGE}:/example hello
     * Spaces between words are allowed. However ":" character indicates the separation between placeholder
     * and replacement and it should only appear once in the string.
     * @param path path of message
     * @param commandSender who to send this message too
     * @param placeholders string array of place holders.
     */
    public void send(String path, CommandSender commandSender, String... placeholders) {
        String message = read(path, placeholders);
        commandSender.sendMessage(message);
    }

    private String replacePlaceholders(String message, String... placeholders) {
        for (String raw : placeholders) {
            String[] parts = raw.split(":");

            if (parts.length != 2) {
                continue;
            }

            String placeholder = parts[0];
            String replacement = parts[1];

            message = message.replace(placeholder, replacement);
        }
        return message;
    }

}
