package me.modify.pocketworld.theme;

import me.modify.pocketworld.PocketWorldPlugin;

import java.util.*;

/**
 * Represents the set of all themes available on the server.
 * All themes are cached here from data source upon server start.
 */
public class ThemeRegistry {

    private final PocketWorldPlugin plugin;
    private Set<PocketTheme> registry;

    public ThemeRegistry(PocketWorldPlugin plugin) {
        this.plugin = plugin;
        this.registry = new HashSet<>();
    }

    public void load() {
        registry.addAll(plugin.getDataSource().getConnection().getDAO().getAllPocketThemes());
    }

    public void register(PocketTheme theme) {
        registry.add(theme);
    }

    public void reload() {
        registry.clear();
        load();
    }

    public List<PocketTheme> getThemes() {
        return new ArrayList<>(registry);
    }

    public PocketTheme getThemeByID(UUID id) {
        Optional<PocketTheme> theme = registry.stream().filter(t -> t.getId().equals(id)).findFirst();
        return theme.orElse(null);
    }

}
