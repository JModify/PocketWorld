package me.modify.pocketworld.data;

import me.modify.pocketworld.theme.PocketTheme;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.world.PocketWorld;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
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
     * <p>
     * Any changes made to a PocketWorld object should be followed by this method so that the data source
     * recognises these changes and persists them across server restarts.
     * @param world pocket world to update in the data source.
     */
    void updatePocketWorld(PocketWorld world);


    /**
     * Registers a pocket user with the data source.
     * This method should follow newly created pocket user objects (i.e. new players who join the server).
     * @param userId
     */
    void registerPocketUser(UUID userId);

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
     * Retrieves all pocket worlds stored in the data source which are associated to a user.
     * A user is "associated" to a pocket world if they are a member, mod or owner.
     * @param userId id of user to retrieve worlds.
     * @return an ArrayList of all pocket worlds the user is associated too.
     */
    List<PocketWorld> getPocketWorlds(UUID userId);

    /**
     * Returns a count of pocket worlds the user is associated with.
     * @param userId id of user to retrieve this count.
     * @return an integer representing the number of pocket worlds a user is associated with.
     */
    int countPocketWorlds(UUID userId);

    /**
     * Saves the current state of a user's inventory to the data source.
     * User inventories are encoded into a single string using Base64 through this method.
     * @param userId id of user to save inventory.
     * @param inventory inventory to save.
     */
    void saveUserInventory(UUID userId, Inventory inventory);

    /**
     * Retrieves a users inventory from the data source.
     * User inventories are decoded from Base64 then added to a ItemStack array through this method.
     * <p>
     * This method does NOT actually restore a user's inventory to them, it only retrieves the data.
     * Inventory.setContents() should follow this method to avoid data loss.
     * @param userId id of user to retrieve inventory.
     * @return ItemStack array of the user's inventory.
     */
    ItemStack[] retrieveUserInventory(UUID userId);

}
