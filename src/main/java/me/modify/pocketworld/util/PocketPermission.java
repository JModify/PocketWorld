package me.modify.pocketworld.util;

import lombok.Getter;
import org.bukkit.entity.Player;

public enum PocketPermission {

    // Access to use PocketWorld command (/pocketworld)
    POCKET_WORLD_DEFAULT("pocketworld.command.default"),

    // Access to use PocketWorld admin command (/pocketworldadmin)
    POCKET_WORLD_ADMIN("pocketworld.command.admin"),

    // Access to create PocketWorld themes.
    POCKET_WORLD_THEME_CREATE("pocketworld.theme.create"),

    // Access to manage PocketWorld themes.
    POCKET_WORLD_THEME_MANAGE("pocketworld.theme.manage");

    @Getter private String node;
    PocketPermission(String node) {
        this.node = node;
    }

    public static boolean has(Player player, PocketPermission permission) {
        return player.hasPermission(permission.getNode());
    }
}
