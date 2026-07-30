package com.pocketworld.plugin.util;

import org.bukkit.entity.Player;

public enum PocketPermission {

    /** Access to use /pocketworld. */
    POCKET_WORLD_DEFAULT("pocketworld.command.default"),

    /** Access to use /pocketworldadmin. */
    POCKET_WORLD_ADMIN("pocketworld.command.admin"),

    /** Access to create PocketWorld themes. */
    POCKET_WORLD_THEME_CREATE("pocketworld.theme.create"),

    /** Access to manage PocketWorld themes. */
    POCKET_WORLD_THEME_MANAGE("pocketworld.theme.manage");

    private final String node;

    PocketPermission(String node) {
        this.node = node;
    }

    public String node() {
        return node;
    }

    public static boolean has(Player player, PocketPermission permission) {
        return player.hasPermission(permission.node());
    }
}
