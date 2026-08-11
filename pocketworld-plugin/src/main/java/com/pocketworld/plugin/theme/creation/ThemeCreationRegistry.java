package com.pocketworld.plugin.theme.creation;

import com.pocketworld.plugin.PocketWorldPlugin;
import org.bukkit.entity.Player;

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

    /**
     * @return false if {@code userId} was already mid-creation and nothing happened, true if a new
     *         controller was started. Guarding against a duplicate start is essential, not just
     *         tidiness: {@link ThemeCreationController} has no {@code equals()}/{@code hashCode()}
     *         override, so a second controller for the same user would sit in this {@link HashSet}
     *         as a distinct entry (reference equality) alongside the first. {@link #find} then
     *         returns whichever of the two a plain {@code HashSet} happens to iterate to first -
     *         unpredictable, and different calls could resolve to different controllers. Cancelling
     *         or completing one via {@link #removeByController} only ever removes that one specific
     *         instance, so the other is silently orphaned in this static, server-lifetime singleton
     *         with no other path to ever remove it - the exact "stuck until restart" state a second
     *         {@code /theme create} while already mid-creation was observed to cause live. The new
     *         controller's own {@link ThemeCreationController#start()} would also have re-stashed the
     *         player's inventory over the first (real) backup, permanently losing it.
     */
    public boolean addCreator(PocketWorldPlugin plugin, UUID userId) {
        if (containsUser(userId)) {
            return false;
        }

        ThemeCreationController controller = new ThemeCreationController(plugin, userId);
        if (controller.start()) {
            controllers.add(controller);
            return true;
        }
        return false;
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

    /**
     * Call at the top of every PocketWorld command's {@code onCommand()}: a player mid-theme-creation
     * can't meaningfully use any other command (their inventory is stashed, they may be standing in
     * a temporary editor world, and {@code /theme create} itself would otherwise silently corrupt
     * state - see {@link #addCreator}'s javadoc). Sends a reminder and returns true if this player
     * should be blocked; the caller should return immediately when this is true.
     */
    public boolean blocksCommand(PocketWorldPlugin plugin, Player player) {
        if (!containsUser(player.getUniqueId())) {
            return false;
        }
        plugin.getMessageReader().send("theme-creation-blocks-commands", player);
        return true;
    }

    private Optional<ThemeCreationController> find(UUID userId) {
        return controllers.stream().filter(controller -> controller.getUserId().equals(userId)).findFirst();
    }
}
