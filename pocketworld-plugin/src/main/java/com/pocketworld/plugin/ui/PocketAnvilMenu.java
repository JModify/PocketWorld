package com.pocketworld.plugin.ui;

import com.pocketworld.plugin.PocketWorldPlugin;
import org.bukkit.entity.Player;

public abstract class PocketAnvilMenu {

    protected final Player player;
    protected final PocketWorldPlugin plugin;

    public PocketAnvilMenu(Player player, PocketWorldPlugin plugin) {
        this.player = player;
        this.plugin = plugin;
    }

    public abstract void open();
}
