package me.modify.pocketworld.data;

import me.modify.pocketworld.theme.PocketTheme;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.world.PocketWorld;
import org.bson.conversions.Bson;

import java.util.Set;
import java.util.UUID;

/**
 * Data access object.
 * Classes which implement DAO are expected to access a data source
 * using the respective methods.
 */
public interface DAO {

    /**
     * Registers a pocket world with the data source.
     * This method should follow newly created pocket worlds.
     * @param world pocket world to register
     */
    void registerPocketWorld(PocketWorld world);

    /**
     * Retrieves a pocket world with the given world ID.
     * <p>
     * This method should only be used if a world is UNLOADED.
     * If a world is LOADED, the pocket world should be retrieves from the PocketWorldRegistry.
     * @param worldId UUID of the world
     * @return the PocketWorld with the associated ID directly from the data source.
     */
    PocketWorld getPocketWorld(UUID worldId);

    /**
     * Updates the data source with respect to a pocket world.
     * @param world pocket world to update in the data source.
     */
    void updatePocketWorld(PocketWorld world);

    /**
     * Registers a pocket user with the data source.
     * This method should follow newly created pocket user objects (i.e. new players who join the server).
     * @param userId ID of player
     * @param username username of player
     * @return true if the user did not exist (registration was successful), else false.
     */
    boolean registerPocketUser(UUID userId, String username);

    /**
     * Retrieves a PocketUser with the given ID from the data source.
     * @param userId id of user to retrieve
     * @return PocketUser object for the user with this ID.
     */
    PocketUser getPocketUser(UUID userId);

    /**
     * Updates the data source with respect to a pocket user.
     * <p>
     * Any changes made to a PocketUser object should be followed by this method so that the data source
     * recognises these changes and persists them across server restarts.
     * @param user pocket user to update in the data source.
     */
    void updatePocketUser(PocketUser user);

    /**
     * Registers a pocket theme with the data source.
     * This method should follow newly created pocket themes.
     * @param theme
     */
    void registerPocketTheme(PocketTheme theme);

    /**
     * Retrieves a PocketTheme with the given ID from the data source.
     * @deprecated themes should be retrieved from ThemeCache which caches all themes upon server start.
     * @param themeId id of theme to retrieve
     * @return PocketTheme object for the theme with this ID.
     */
    @Deprecated
    PocketTheme getPocketTheme(UUID themeId);

    /**
     * Retrieves all pocket themes stored in the data source.
     * @return a HashSet of all pocket themes
     */
    Set<PocketTheme> getAllPocketThemes();

    /**
     * Deletes a theme from data source.
     * @param themeId id of theme to delete
     */
    void deleteTheme(UUID themeId);
}
