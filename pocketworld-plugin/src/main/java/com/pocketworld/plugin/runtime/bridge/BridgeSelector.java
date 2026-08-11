package com.pocketworld.plugin.runtime.bridge;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.runtime.bridge.anvil.AnvilShadowBridge;

import java.util.List;

/**
 * Picks the best available {@link WorldRuntimeBridge} for the running server once, at startup.
 * Version-specific NMS bridges (added in later stages) are tried first and only trusted after a
 * runtime fingerprint check confirms their expected internals actually exist; this always falls
 * back to {@link AnvilShadowBridge}, which works unmodified on any Paper version.
 */
public final class BridgeSelector {

    private final WorldRuntimeBridge active;

    public BridgeSelector(PocketWorldPlugin plugin) {
        List<WorldRuntimeBridge> candidates = List.of(
                // Version-specific NMS bridges are prepended here in later stages, tried first.
                new AnvilShadowBridge(plugin));

        WorldRuntimeBridge selected = candidates.stream()
                .filter(WorldRuntimeBridge::isAvailable)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No WorldRuntimeBridge is available - AnvilShadowBridge should always be, so this is a bug"));

        this.active = selected;
        if (selected instanceof AnvilShadowBridge) {
            plugin.getLogger().info("PocketWorld: no version-specific runtime bridge for this server; "
                    + "using the Anvil-shadow fallback (slower world load/unload, fully functional).");
        } else {
            plugin.getLogger().info("PocketWorld: using runtime bridge \"" + selected.name() + "\"");
        }
    }

    public WorldRuntimeBridge current() {
        return active;
    }
}
