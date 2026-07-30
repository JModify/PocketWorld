package com.pocketworld.plugin.data.config;

import com.pocketworld.plugin.PocketWorldPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public abstract class PluginFile {

    private final String name;
    private final PocketWorldPlugin plugin;
    private FileConfiguration yaml;
    private File file;

    public PluginFile(PocketWorldPlugin plugin, String name) {
        this.name = name;
        this.plugin = plugin;
        startup();
    }

    public String getFileName() {
        return name.toLowerCase() + ".yml";
    }

    private void startup() {
        file = new File(plugin.getDataFolder(), getFileName());
        createIfNotExists();
        reload();
    }

    public void reload() {
        if (file == null) {
            startup();
        }

        yaml = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getYaml() {
        if (yaml == null) {
            reload();
        }

        return yaml;
    }

    public boolean save() {
        if (file == null || yaml == null) {
            reload();
        }

        try {
            yaml.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save file " + getFileName() + ": " + e.getMessage());
            return false;
        }
    }

    private void createIfNotExists() {
        if (!file.exists()) {
            plugin.saveResource(getFileName(), false);
        }
    }
}
