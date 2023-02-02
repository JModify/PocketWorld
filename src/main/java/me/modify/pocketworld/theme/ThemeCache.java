package me.modify.pocketworld.theme;

import me.modify.pocketworld.PocketWorldPlugin;

import java.util.*;

/**
 * Represents the set of all themes available on the server.
 * All themes are cached here from data source upon server start.
 */
public class ThemeCache {

    private final PocketWorldPlugin plugin;

    private Set<PocketTheme> cache;

    public ThemeCache(PocketWorldPlugin plugin) {
        this.plugin = plugin;
        this.cache = new HashSet<>();
    }

    public void load() {
        cache.addAll(plugin.getDataSource().getConnection().getDAO().getAllPocketThemes());
    }

    public void cacheTheme(PocketTheme theme) {
        cache.add(theme);
    }

    public void reload() {
        cache.clear();
        load();
    }

    public List<PocketTheme> getThemes() {
        List<PocketTheme> copy = new ArrayList<>(cache);
        return copy;
    }

    public PocketTheme getThemeByID(UUID id) {
        Optional<PocketTheme> theme = cache.stream().filter(t -> t.getId().equals(id)).findFirst();
        return theme.orElse(null);
    }

}
