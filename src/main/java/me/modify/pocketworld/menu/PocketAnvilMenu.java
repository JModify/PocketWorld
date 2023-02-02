package me.modify.pocketworld.menu;

import me.modify.pocketworld.PocketWorldPlugin;
import org.bukkit.entity.Player;

public abstract class PocketAnvilMenu {

    protected Player player;

    public PocketAnvilMenu(Player player) {
        this.player = player;
    }

    public abstract void open();
}
