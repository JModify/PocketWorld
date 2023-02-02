package me.modify.pocketworld.theme.creation;

import lombok.Getter;
import me.modify.pocketworld.PocketWorldPlugin;

import java.util.*;

/**
 * ThemeCreationRegistry keeps track of all admin users creating a theme.
 * A user being added to this registry represents the fact that they are creating a theme and so their inventory
 * is saved to the data source.
 * A user being added to this registry represents the fact that they have finished the creation process and so
 * their inventory is restored.
 * Upon server shutdown or player disconnect, users are removed from this registry and their inventory
 * is to be restored.
 */
public class ThemeCreationRegistry {

    @Getter
    private static final ThemeCreationRegistry instance = new ThemeCreationRegistry();

    public Set<ThemeCreationController> controllers;

    public ThemeCreationRegistry() {
        this.controllers = new HashSet<>();
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
        Optional<ThemeCreationController> controller = controllers.stream()
                .filter(tcc -> tcc.getUserId().equals(userId))
                .findFirst();

        controller.ifPresent(themeCreationController -> controllers.remove(themeCreationController));
    }

    public boolean containsUser(UUID userId) {
        Optional<ThemeCreationController> controller = controllers.stream()
                .filter(tcc -> tcc.getUserId().equals(userId))
                .findFirst();

        return controller.isPresent();
    }

    public ThemeCreationController getController(UUID userId) {
        Optional<ThemeCreationController> controller = controllers.stream()
                .filter(tcc -> tcc.getUserId().equals(userId))
                .findFirst();

        return controller.orElse(null);
    }
}
