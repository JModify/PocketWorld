package me.modify.pocketworld.ui;

import me.modify.pocketworld.PocketWorldPlugin;
import org.bukkit.entity.Player;

public abstract class PocketAnvilMenu {

    protected Player player;
    protected PocketWorldPlugin plugin;

    public PocketAnvilMenu(Player player, PocketWorldPlugin plugin) {
        this.player = player;
        this.plugin = plugin;
    }

    public abstract void open();
}
