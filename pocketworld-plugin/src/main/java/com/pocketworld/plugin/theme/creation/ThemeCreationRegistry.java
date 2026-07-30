package com.pocketworld.plugin.theme.creation;

import com.pocketworld.plugin.PocketWorldPlugin;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks admins currently mid-way through creating a theme. Being added to this registry means
 * their inventory has been stashed for the duration; being removed means it's been restored.
 */
public final class ThemeCreationRegistry {

    private static final ThemeCreationRegistry INSTANCE = new ThemeCreationRegistry();

    private final Set<ThemeCreationController> controllers = new HashSet<>();

    public static ThemeCreationRegistry getInstance() {
        return INSTANCE;
    }

    public void addCreator(PocketWorldPlugin plugin, UUID userId) {
        ThemeCreationController controller = new ThemeCreationController(plugin, userId);
        if (controller.start()) {
            controllers.add(controller);
        }
    }

    public void removeByController(ThemeCreationController controller) {
        controllers.remove(controller);
    }

    public void removeByUser(UUID userId) {
        find(userId).ifPresent(controllers::remove);
    }

    public boolean containsUser(UUID userId) {
        return find(userId).isPresent();
    }

    public ThemeCreationController getController(UUID userId) {
        return find(userId).orElse(null);
    }

    private Optional<ThemeCreationController> find(UUID userId) {
        return controllers.stream().filter(controller -> controller.getUserId().equals(userId)).findFirst();
    }
}
