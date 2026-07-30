package com.pocketworld.plugin.data;

import com.pocketworld.plugin.theme.PocketTheme;
import com.pocketworld.plugin.user.PocketUser;
import com.pocketworld.plugin.world.PocketWorld;

import java.util.Set;
import java.util.UUID;

/** Data access object for this plugin's metadata (users/worlds/themes) - not the Slime world storage. */
public interface DAO {

    /** Registers a newly created pocket world with the data source. */
    void registerPocketWorld(PocketWorld world);

    /** Retrieves a pocket world by id. Should only be used while the world is UNLOADED - a loaded world's canonical state is the cached object. */
    PocketWorld getPocketWorld(UUID worldId);

    void updatePocketWorld(PocketWorld world);

    /** @return true if the user already existed (no registration performed), else false. */
    boolean registerPocketUser(UUID userId, String username);

    PocketUser getPocketUser(UUID userId);

    void updatePocketUser(PocketUser user);

    void registerPocketTheme(PocketTheme theme);

    /** @deprecated themes should be retrieved from ThemeRegistry, which caches all themes at startup. */
    @Deprecated
    PocketTheme getPocketTheme(UUID themeId);

    Set<PocketTheme> getAllPocketThemes();

    void deleteTheme(UUID themeId);
}
