package com.pocketworld.plugin.api;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.util.PocketUtils;
import com.pocketworld.plugin.world.PocketWorld;
import org.bukkit.World;

import java.util.UUID;

final class PocketWorldAPIImpl implements PocketWorldAPI {

    private final PocketWorldPlugin plugin;

    PocketWorldAPIImpl(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public PocketWorld getWorld(UUID worldId) {
        return plugin.getWorldCache().getIfCached(worldId);
    }

    @Override
    public boolean isPocketWorld(World world) {
        return PocketUtils.isUUID(world.getName());
    }
}
