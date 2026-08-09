package com.pocketworld.plugin.util;

import org.bukkit.entity.Player;

/**
 * Every permission node this plugin checks. Each command that takes sub-arguments (`/theme`,
 * `/pocketworldadmin`) has its own base "can you run this command at all" node, plus one further
 * node per sub-argument - so an admin can hand out, say, {@code pocketworld.theme.manage} without
 * also granting {@code pocketworld.theme.delete}. `/pocketworld` has no sub-arguments, so its one
 * node is both the base and only check.
 */
public enum PocketPermission {

    /** Base access to use /pocketworld at all. */
    COMMAND_POCKETWORLD("pocketworld.command.pocketworld"),

    /** Base access to use /theme at all - required in addition to the specific THEME_* node below. */
    COMMAND_THEME("pocketworld.command.theme"),

    /** Base access to use /pocketworldadmin at all - required in addition to the specific ADMIN_* node below. */
    COMMAND_ADMIN("pocketworld.command.admin"),

    /** /theme create */
    THEME_CREATE("pocketworld.theme.create"),

    /** /theme manage (and its "list" alias) */
    THEME_MANAGE("pocketworld.theme.manage"),

    /** /theme delete */
    THEME_DELETE("pocketworld.theme.delete"),

    /** /theme import (not yet implemented) */
    THEME_IMPORT("pocketworld.theme.import"),

    /** /theme edit (not yet implemented) */
    THEME_EDIT("pocketworld.theme.edit"),

    /** /pocketworldadmin reload */
    ADMIN_RELOAD("pocketworld.admin.reload"),

    /** /pocketworldadmin import */
    ADMIN_IMPORT("pocketworld.admin.import"),

    /** /pocketworldadmin export */
    ADMIN_EXPORT("pocketworld.admin.export"),

    /** /pocketworldadmin validate */
    ADMIN_VALIDATE("pocketworld.admin.validate");

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
