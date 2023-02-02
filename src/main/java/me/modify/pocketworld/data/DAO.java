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

    PocketWorld getPocketWorld(UUID worldId);

    void updatePocketWorld(PocketWorld world);

    PocketUser getPocketUser(UUID userId);

    PocketTheme getPocketTheme(UUID themeId);

    void updatePocketUser(PocketUser user);

    Set<PocketTheme> getAllPocketThemes();

    List<PocketWorld> getPocketWorlds(UUID userId);

    int countPocketWorlds(UUID userId);

    void registerPocketUser(UUID userId);

    void registerPocketWorld(PocketWorld world);

    void registerPocketTheme(PocketTheme theme);

    void saveUserInventory(UUID userId, Inventory inventory);

    ItemStack[] retrieveUserInventory(UUID userId);

}
