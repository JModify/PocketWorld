package com.pocketworld.plugin.api;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.world.PocketWorld;
import org.bukkit.World;

import java.util.UUID;

/**
 * Public service for other plugins to integrate with PocketWorld. Registered with Bukkit's
 * {@link org.bukkit.plugin.ServicesManager} under this interface - other plugins fetch it via
 * {@code Bukkit.getServicesManager().load(PocketWorldAPI.class)} rather than depending on this
 * plugin's internal classes directly.
 */
public interface PocketWorldAPI {

    /** The pocket world with this id, if it's currently cached (loaded, or recently accessed) - null otherwise. */
    PocketWorld getWorld(UUID worldId);

    /** Whether the given live Bukkit world is a pocket world's instance. */
    boolean isPocketWorld(World world);

    static PocketWorldAPI create(PocketWorldPlugin plugin) {
        return new PocketWorldAPIImpl(plugin);
    }
}
