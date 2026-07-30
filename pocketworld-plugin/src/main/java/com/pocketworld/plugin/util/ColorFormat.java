package com.pocketworld.plugin.util;

import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Colorizes messages containing chat color codes - supports both hex and legacy Bukkit codes. */
public final class ColorFormat {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#(\\w{5}[0-9a-f])");

    private ColorFormat() {}

    public static String format(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder();

        while (matcher.find()) {
            matcher.appendReplacement(buffer, ChatColor.of("#" + matcher.group(1)).toString());
        }
        matcher.appendTail(buffer);

        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static List<String> formatList(List<String> messages) {
        List<String> result = new ArrayList<>();
        for (String message : messages) {
            result.add(format(message));
        }
        return result;
    }

    public static List<String> stripListColor(List<String> listToStrip) {
        List<String> result = new ArrayList<>();
        for (String s : listToStrip) {
            result.add(ChatColor.stripColor(s));
        }
        return result;
    }
}
