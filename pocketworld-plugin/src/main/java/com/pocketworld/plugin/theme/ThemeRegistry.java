package com.pocketworld.plugin.theme;

import com.pocketworld.plugin.PocketWorldPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** The set of all themes available on the server, cached from the data source at startup. */
public class ThemeRegistry {

    private final PocketWorldPlugin plugin;
    private final Set<PocketTheme> registry = new HashSet<>();

    public ThemeRegistry(PocketWorldPlugin plugin) {
        this.plugin = plugin;
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

    public void delete(UUID id) {
        registry.removeIf(theme -> theme.getId().equals(id));
    }

    public List<PocketTheme> getThemes() {
        return new ArrayList<>(registry);
    }

    public PocketTheme getThemeByID(UUID id) {
        Optional<PocketTheme> theme = registry.stream().filter(t -> t.getId().equals(id)).findFirst();
        return theme.orElse(null);
    }
}
