package me.modify.pocketworld.theme.creation;

import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.exceptions.WorldAlreadyExistsException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import lombok.Getter;
import lombok.Setter;
import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.theme.PocketTheme;
import me.modify.pocketworld.user.UserInventory;
import me.modify.pocketworld.util.ColorFormat;
import me.modify.pocketworld.util.PocketItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Consumer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents the controlling object for a player creating a theme.
 */
public class ThemeCreationController {

    private final PocketWorldPlugin plugin;

    @Getter
    private final UUID userId;

    private UUID themeId;

    @Getter @Setter
    private String name;

    @Getter @Setter
    private String biome;

    @Getter @Setter
    private Material icon;

    @Getter @Setter
    private String description;

    @Getter @Setter
    private String spawnPoint;

    @Getter @Setter
    private ThemeCreationState state;

    private BukkitTask editorWorldGenerationTask;

    public ThemeCreationController(PocketWorldPlugin plugin, UUID userId) {
        this.plugin = plugin;
        this.userId = userId;
        this.editorWorldGenerationTask = null;
        this.themeId = null;
    }

    /**
     * Starts the theme creation process. Responsible for the following:
     * - Saving user inventory.
     * - Granting user initial items for theme creation.
     *
     * @return true if actions executed successfully, else false.
     */
    public boolean start() {
        PocketItem nameEntry = new PocketItem.Builder(plugin)
                .material(Material.PAPER)
                .stackSize(1)
                .displayName("&6&lTheme Name Entry")
                .lore(List.of("&7Right click to enter theme name."))
                .tag("is-name-entry")
                .build();

        PocketItem cancel = new PocketItem.Builder(plugin)
                .material(Material.BARRIER)
                .stackSize(1)
                .displayName("&c&lCancel")
                .lore(List.of("&7Click to cancel theme creation."))
                .tag("is-cancel-theme")
                .build();

        Player player = Bukkit.getPlayer(userId);

        // Player is not online. Controller should be removed from registry.
        if (player == null) {
            return false;
        }

        // Add NBT data to player flagging them as having an inventory which needs to be restored.
        UserInventory.saveUserInventory(plugin, player);

        player.getInventory().clear();
        player.getInventory().setItem(4, nameEntry.get());
        player.getInventory().setItem(8, cancel.get());
        player.getInventory().setHeldItemSlot(4);
        this.state = ThemeCreationState.SELECT_NAME;
        return true;
    }

    /**
     * Attempts to move this theme creation process to the next state.
     * <p>
     * In the case where this method returns false, it is expected that
     * the theme controller should be removed from the ThemeCreationRegistry.
     */
    public void nextState() {
        Player player = Bukkit.getPlayer(userId);
        if (player == null) {
            return;
        }

        ItemStack cancel = new PocketItem.Builder(plugin)
                .material(Material.BARRIER)
                .stackSize(1)
                .displayName("&c&lCancel")
                .lore(List.of("&7Click to cancel theme creation."))
                .tag("is-cancel-theme")
                .build().get();

        //TODO: Add sound effects when new state starts

        // Sets up the next state depending on the current state.
        switch(state) {
            case SELECT_NAME -> {
                PocketItem biomeSelect = new PocketItem.Builder(plugin)
                        .material(Material.GRASS_BLOCK)
                        .displayName("&6&lBiome Select")
                        .lore(List.of("&7Right click to select theme biome."))
                        .tag("is-biome-select")
                        .build();
                player.getInventory().clear();
                player.getInventory().setItem(4, biomeSelect.get());
                player.getInventory().setItem(8, cancel);
                player.getInventory().setHeldItemSlot(4);
                this.state = ThemeCreationState.SELECT_BIOME;
            }
            case SELECT_BIOME -> {
                PocketItem selectIcon = new PocketItem.Builder(plugin)
                        .material(Material.BEDROCK)
                        .displayName("&6&lIcon Select")
                        .lore(List.of("&7Select an icon to represent your theme."))
                        .tag("is-icon-select")
                        .build();
                player.getInventory().clear();
                player.getInventory().setItem(4, selectIcon.get());
                player.getInventory().setItem(8, cancel);
                player.getInventory().setHeldItemSlot(4);
                this.state = ThemeCreationState.SELECT_ICON;
            }

            case SELECT_ICON -> {
                List<String> message = new ArrayList<>();
                message.add("&7&m---------------------------------");
                message.add("&6&lEnter Theme Description");
                message.add("&eType a description for your theme in chat to continue.");
                message.add("&7This description is visible to players when creating a PocketWorld.");
                message.add("&7&m---------------------------------");

                ColorFormat.formatList(message).forEach(player::sendMessage);

                // Next state requires chat input. No items other than "cancel" item to be added.
                player.getInventory().clear();
                player.getInventory().setItem(8, cancel);
                player.getInventory().setHeldItemSlot(4);
                this.state = ThemeCreationState.ENTER_DESCRIPTION;
            }

            case ENTER_DESCRIPTION -> {
                PocketItem generatingItem = new PocketItem.Builder(plugin)
                        .material(Material.ENDER_PEARL)
                        .displayName("&6&lGenerating Editor World...")
                        .lore(List.of("&7Please wait!"))
                        .build();

                player.getInventory().clear();
                player.getInventory().setItem(4, generatingItem.get());
                player.getInventory().setItem(8, cancel);
                player.getInventory().setHeldItemSlot(4);
                generateEditorWorld();
                this.state = ThemeCreationState.GENERATING_WORLD;
            }

            case GENERATING_WORLD -> {
                PocketItem spawnPointItem = new PocketItem.Builder(plugin)
                        .material(Material.ENDER_EYE)
                        .displayName("&d&lSpawn Point")
                        .lore(List.of("&7Click to set default spawn point of this theme",
                                "&7to your current location."))
                        .tag("is-spawn-point")
                        .build();

                player.getInventory().clear();
                player.getInventory().setItem(4, spawnPointItem.get());
                player.getInventory().setItem(8, cancel);
                player.getInventory().setHeldItemSlot(4);
                this.state = ThemeCreationState.SET_SPAWN;
            }
            case SET_SPAWN -> {
                PocketItem completeItem = new PocketItem.Builder(plugin)
                        .material(Material.LIME_WOOL)
                        .displayName("&a&lDone")
                        .lore(List.of("&7Click to save and complete creation process."))
                        .tag("is-theme-complete")
                        .build();

                player.getInventory().clear();
                player.getInventory().setItem(7, completeItem.get());
                player.getInventory().setItem(8, cancel);
                player.getInventory().setHeldItemSlot(7);
                this.state = ThemeCreationState.BUILDING;
            }
            case BUILDING -> completeCreation();
        }

    }

    /**
     * Retrieves the slime property map for the editor world associated with this controller.
     *
     * @return slime property map of editor world.
     */
    private SlimePropertyMap getEditorWorldPropertyMap() {
        SlimePropertyMap propertyMap = new SlimePropertyMap();

        propertyMap.setValue(SlimeProperties.SPAWN_X, 0);
        propertyMap.setValue(SlimeProperties.SPAWN_Y, 100);
        propertyMap.setValue(SlimeProperties.SPAWN_Z, 0);
        propertyMap.setValue(SlimeProperties.ALLOW_ANIMALS, false);
        propertyMap.setValue(SlimeProperties.ALLOW_MONSTERS, false);
        propertyMap.setValue(SlimeProperties.PVP, false);
        propertyMap.setValue(SlimeProperties.DIFFICULTY, "normal");
        propertyMap.setValue(SlimeProperties.DEFAULT_BIOME, "minecraft:" + biome);
        return propertyMap;
    }

    /**
     * Generates an editor world for this theme.
     */
    private void generateEditorWorld() {
        this.themeId = UUID.randomUUID();

        SlimePropertyMap propertyMap = getEditorWorldPropertyMap();
        SlimePlugin slime = plugin.getSlimeHook().getAPI();
        SlimeLoader loader = slime.getLoader("theme");

        Player player = Bukkit.getPlayer(userId);

        long startTime = System.currentTimeMillis();
        Consumer<Boolean> postLoadActions = outcome -> {
            if (outcome) {
                World world = Bukkit.getWorld(themeId.toString());

                if (world != null) world.getBlockAt(0, 64, 0).setType(Material.BEDROCK);

                long time = System.currentTimeMillis() - startTime;
                if (player != null) {
                    player.sendMessage(ColorFormat.format("&aSuccessfully generated editor world in " + time + "ms"));
                    player.teleport(new Location(world, 0, 66, 0));
                }
                nextState();
            }
        };

        Consumer<Exception> generationFailure = e -> {
            plugin.getLogger().severe("Failed to generate editor world for " + themeId.toString() + " theme.");
            e.printStackTrace();
        };

        editorWorldGenerationTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                SlimeWorld themeWorld = slime.createEmptyWorld(loader, themeId.toString(), false, propertyMap);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        slime.generateWorld(themeWorld);
                        postLoadActions.accept(true);
                    } catch (IllegalArgumentException e) {
                        generationFailure.accept(e);
                    }
                });
            } catch (WorldAlreadyExistsException | IOException e) {
                generationFailure.accept(e);
            }
        });
    }

    /**
     * Cancels the theme creation process.
     */
    public void cancelCreation() {
        // Restore inventory if player is online, else inventory is to be restored on next login.
        Player player = Bukkit.getPlayer(userId);
        if (player != null) {
            UserInventory.restoreUserInventory(plugin, player);
        }

        if (state == ThemeCreationState.GENERATING_WORLD) {
            // Cancel the editor world generation process
            editorWorldGenerationTask.cancel();
        } else if (state == ThemeCreationState.BUILDING) {

            // If player is online, first teleport them to default world spawn.
            if (player != null) {
                World defaultWorld = plugin.getServer().getWorlds().get(0);
                player.teleport(defaultWorld.getSpawnLocation());
            }

            // Delete the theme world
            SlimePlugin slime = plugin.getSlimeHook().getAPI();
            SlimeLoader loader = slime.getLoader("theme");

            try {
                loader.deleteWorld(themeId.toString());
            } catch (UnknownWorldException | IOException e) {
                e.printStackTrace();
            }
        }

        ThemeCreationRegistry.getInstance().removeByController(this);
    }

    public void completeCreation() {
        PocketTheme theme = new PocketTheme(themeId, name, description, spawnPoint, biome, icon);
        plugin.getDataSource().getConnection().getDAO().registerPocketTheme(theme);
        plugin.getThemeRegistry().register(theme);
        ThemeCreationRegistry.getInstance().removeByController(this);

        Player player = Bukkit.getPlayer(userId);

        if (player != null) {
            World defaultWorld = plugin.getServer().getWorlds().get(0);
            player.teleport(defaultWorld.getSpawnLocation());

            UserInventory.restoreUserInventory(plugin, player);
        }

        // Unlock the world so that it can be saved
        SlimeLoader loader = plugin.getSlimeHook().getAPI().getLoader("theme");
        try {
            loader.unlockWorld(themeId.toString());
        } catch (UnknownWorldException | IOException e) {
            e.printStackTrace();
        }

        World bWorld = Bukkit.getWorld(themeId.toString());
        if (bWorld != null) Bukkit.unloadWorld(bWorld, true);
    }

    public void handleChatInput(String message) {
        if (state != ThemeCreationState.ENTER_DESCRIPTION) {
            return;
        }

        //TODO: Maybe some restrictions later down the road for theme descriptions. i.e. length
        setDescription(message);
        nextState();
    }

    /**
     * Represents the state of the creation process this controller controls.
     */
    private enum ThemeCreationState {
        SELECT_NAME, SELECT_BIOME, SELECT_ICON, ENTER_DESCRIPTION, GENERATING_WORLD, SET_SPAWN, BUILDING
    }

}
