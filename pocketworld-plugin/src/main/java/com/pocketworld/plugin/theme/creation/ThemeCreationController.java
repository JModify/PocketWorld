package com.pocketworld.plugin.theme.creation;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.theme.PocketTheme;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.user.PocketUserInventory;
import com.pocketworld.plugin.util.ColorFormat;
import com.pocketworld.plugin.runtime.WorldProperties;
import com.pocketworld.plugin.runtime.bridge.anvil.LevelDatWriter;
import com.pocketworld.plugin.util.VoidGenerator;
import com.pocketworld.plugin.world.PocketWorld;
import com.pocketworld.slime.model.SlimeWorldData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/** The controlling object for one admin's in-progress theme creation. */
public class ThemeCreationController {

    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z0-9 ]+", Pattern.CASE_INSENSITIVE);

    private final PocketWorldPlugin plugin;
    private final UUID userId;

    private UUID themeId;
    private String name;
    private String biome;
    private Material icon;
    private String description;
    private String spawnPoint;
    private ThemeCreationState state;

    private BukkitTask editorWorldGenerationTask;
    /** Set as soon as {@link #cancelCreation()}/{@link #completeCreation()} starts, before either
     *  teleports the player out of the editor world - both of those teleports would otherwise be
     *  indistinguishable, from a listener's perspective, from the player unexpectedly leaving the
     *  editor world some other way, and re-triggering a cancel reentrant into this same call. */
    private boolean endingCreation;

    public ThemeCreationController(PocketWorldPlugin plugin, UUID userId) {
        this.plugin = plugin;
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBiome() {
        return biome;
    }

    public void setBiome(String biome) {
        this.biome = biome;
    }

    public Material getIcon() {
        return icon;
    }

    public void setIcon(Material icon) {
        this.icon = icon;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSpawnPoint() {
        return spawnPoint;
    }

    public void setSpawnPoint(String spawnPoint) {
        this.spawnPoint = spawnPoint;
    }

    public UUID getThemeId() {
        return themeId;
    }

    /** Whether {@link #cancelCreation()} or {@link #completeCreation()} has already started for
     *  this controller - see the {@code endingCreation} field javadoc for why this matters. */
    public boolean isEnding() {
        return endingCreation;
    }

    /** Whether the player has actually been teleported into the editor world at this point in the
     *  flow - true only once {@code SET_SPAWN} or {@code BUILDING} has been reached. Used to tell
     *  whether an unexpected world change means they left the editor world (worth reacting to) or
     *  they simply haven't been teleported in yet (nothing to react to). */
    public boolean isPhysicallyInEditorWorld() {
        return state == ThemeCreationState.SET_SPAWN || state == ThemeCreationState.BUILDING;
    }

    /** Stashes the admin's inventory and prompts them to type a theme name in chat. Returns false if they're offline. */
    public boolean start() {
        Player player = Bukkit.getPlayer(userId);
        if (player == null) {
            return false;
        }

        PocketUserInventory.saveUserInventory(plugin, player);

        player.getInventory().clear();
        player.getInventory().setItem(8, cancelItem().get());
        player.getInventory().setHeldItemSlot(4);

        List<String> prompt = List.of(
                "&6&lEnter Theme Name",
                "&eType a name for your theme in chat to continue.");
        ColorFormat.formatList(prompt).forEach(player::sendMessage);

        this.state = ThemeCreationState.SELECT_NAME;
        return true;
    }

    /** Advances to the next state of the creation process. */
    public void nextState() {
        Player player = Bukkit.getPlayer(userId);
        if (player == null) {
            return;
        }

        ItemStack cancel = cancelItem().get();

        switch (state) {
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
                player.getInventory().setHeldItemSlot(1);
                this.state = ThemeCreationState.BUILDING;
            }
            case BUILDING -> {
                plugin.getMessageReader().send("theme-creation-complete", player, "{NAME}:" + name);
                completeCreation();
            }
        }
    }

    private PocketItem cancelItem() {
        return new PocketItem.Builder(plugin)
                .material(Material.BARRIER)
                .stackSize(1)
                .displayName("&c&lCancel")
                .lore(List.of("&7Click to cancel theme creation."))
                .tag("is-cancel-theme")
                .build();
    }

    /**
     * Generates a fresh, empty (void) Bukkit world for the admin to build the theme in - an
     * intentionally blank canvas, not a fully terrain-generated world that happens to be
     * biome-locked, so nothing ends up in a theme's stored data except what was actually built. The
     * world border matches {@link PocketWorld#DEFAULT_WORLD_SIZE}, the same size a world created
     * from this theme will actually get, so a theme creator builds within the real bounds a player
     * will experience. Unlike loading an existing PocketWorld, there's no stored Slime data to
     * materialize yet - this world only becomes part of the Slime storage layer once
     * {@link #completeCreation()} extracts and persists it.
     */
    private void generateEditorWorld() {
        this.themeId = UUID.randomUUID();
        long startTime = System.currentTimeMillis();
        Player player = Bukkit.getPlayer(userId);

        editorWorldGenerationTask = Bukkit.getScheduler().runTask(plugin, () -> {
            // Pre-seed level.dat with a known spawn before Bukkit.createWorld() ever runs: without
            // this, vanilla's own first-creation spawn search touches (and permanently persists) a
            // large fixed radius of untouched void chunks around origin, unconditionally - confirmed
            // empirically, and independent of keepSpawnLoaded/world-border timing. See LevelDatWriter.
            Path worldFolder = Bukkit.getWorldContainer().toPath().resolve(themeId.toString());
            try {
                Files.createDirectories(worldFolder);
                LevelDatWriter.write(worldFolder, themeId.toString(), Bukkit.getUnsafe().getDataVersion(),
                        new WorldProperties(0.5, 100.0, 0.5, 0f, 0f, "normal", false));
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to pre-seed editor world level.dat for theme " + themeId, e);
            }

            WorldCreator creator = new WorldCreator(themeId.toString())
                    .environment(World.Environment.NORMAL)
                    .generator(new VoidGenerator())
                    .biomeProvider(new SingleBiomeProvider(biome))
                    .generateStructures(false);
            World world = Bukkit.createWorld(creator);
            if (world == null) {
                plugin.getLogger().severe("Failed to generate editor world for theme " + themeId);
                return;
            }

            // setKeepSpawnInMemory is plain Bukkit/Spigot World API (unlike WorldCreator#keepSpawnLoaded,
            // which is Paper-only), so it's set here after creation rather than on the WorldCreator chain.
            world.setKeepSpawnInMemory(false);
            world.setSpawnFlags(false, false);
            world.setPVP(false);
            world.setDifficulty(org.bukkit.Difficulty.NORMAL);

            // The origin chunk's very first-ever touch in a brand-new world forces Paper's own
            // chunk-generation pipeline through an expensive, wide-radius pass - confirmed empirically
            // to cost over a second of main-thread blocking. Unlike real pocket-world creation (the hot,
            // frequent, per-player path - see AnvilSlotPool), editor-world creation is a rare, admin-only
            // action, so it isn't worth Paper-only getChunkAtAsync's async pre-warm just to keep this one
            // path portable to Spigot too; touching it synchronously here is an acceptable one-off cost.
            world.getChunkAt(0, 0);

            // The player may have disconnected (or explicitly cancelled) while the chunk touch above
            // was running - editorWorldGenerationTask.cancel() in cancelCreation() only stops this outer
            // sync task, so without this check the world could still get fully set up after the
            // controller itself was already removed from the registry, with nothing left to clean it up.
            if (!ThemeCreationRegistry.getInstance().containsUser(userId)) {
                discardEditorWorld(world);
                return;
            }

            world.setSpawnLocation(0, 100, 0);
            world.getBlockAt(0, 99, 0).setType(Material.BEDROCK);
            world.getWorldBorder().setCenter(0.0, 0.0);
            world.getWorldBorder().setSize(PocketWorld.DEFAULT_WORLD_SIZE);

            long time = System.currentTimeMillis() - startTime;
            if (player != null) {
                plugin.getMessageReader().send("theme-editor-world-generated", player, "{TIME}:" + time);
                player.teleport(new Location(world, 0.5, 100, 0.5));
            }
            nextState();
        });
    }

    /** Cancels the theme creation process, discarding any in-progress editor world. */
    public void cancelCreation() {
        if (endingCreation) {
            return;
        }
        endingCreation = true;

        Player player = Bukkit.getPlayer(userId);
        if (player != null) {
            PocketUserInventory.restoreUserInventory(plugin, player);
        }

        if (state == ThemeCreationState.GENERATING_WORLD) {
            // Only stops the outer sync task - if generation has already reached its async
            // continuation, that continuation checks the registry itself (see generateEditorWorld())
            // and discards the world once this method removes the controller from it below.
            editorWorldGenerationTask.cancel();
        } else if (state == ThemeCreationState.SET_SPAWN || state == ThemeCreationState.BUILDING) {
            // Both states mean the player was actually teleported into the editor world already
            // (SET_SPAWN immediately after generation, BUILDING once they've confirmed a spawn) -
            // leaving it loaded here would silently orphan it forever, since nothing else is
            // tracking it once this controller is removed from the registry below.
            if (player != null) {
                World defaultWorld = plugin.getServer().getWorlds().get(0);
                player.teleport(defaultWorld.getSpawnLocation());
            }

            World editorWorld = Bukkit.getWorld(themeId.toString());
            if (editorWorld != null) {
                discardEditorWorld(editorWorld);
            }
        }

        ThemeCreationRegistry.getInstance().removeByController(this);
    }

    /** Unloads (without saving) an editor world that's being discarded rather than completed -
     *  shared by cancelCreation() and the generation-race guard in generateEditorWorld(). Like
     *  completeCreation(), the caller must ensure the player is out of the world first (or was
     *  never teleported in) - Bukkit.unloadWorld() refuses to unload a world that still has players
     *  in it, and this only logs that failure rather than retrying. */
    private void discardEditorWorld(World editorWorld) {
        try {
            plugin.getThemeRuntime().unloadSync(editorWorld, themeId.toString(), false);
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("Failed to discard cancelled theme editor world " + themeId + ": " + e);
        }
    }

    /** Extracts the finished editor world, persists it as the theme's stored world, and registers the theme. */
    public void completeCreation() {
        if (endingCreation) {
            return;
        }
        endingCreation = true;

        World editorWorld = Bukkit.getWorld(themeId.toString());
        Player player = Bukkit.getPlayer(userId);

        // Must happen before unloadSync(): Bukkit.unloadWorld() refuses (and this whole extraction
        // fails) if the world still has a player standing in it.
        if (player != null) {
            World defaultWorld = plugin.getServer().getWorlds().get(0);
            player.teleport(defaultWorld.getSpawnLocation());
            PocketUserInventory.restoreUserInventory(plugin, player);
        }

        SlimeWorldData data = null;
        if (editorWorld != null) {
            try {
                data = plugin.getThemeRuntime().unloadSync(editorWorld, themeId.toString(), true);
            } catch (java.io.IOException e) {
                plugin.getLogger().severe("Failed to extract theme editor world " + themeId + ": " + e);
            }
        }

        SlimeWorldData toPersist = data;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (toPersist != null) {
                try {
                    plugin.getThemeRuntime().persist(themeId.toString(), toPersist);
                } catch (java.io.IOException e) {
                    plugin.getLogger().severe("Failed to persist theme " + themeId + ": " + e);
                    return;
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                PocketTheme theme = new PocketTheme(themeId, name, description, spawnPoint, biome, icon);
                plugin.getDataSource().getConnection().getDAO().registerPocketTheme(theme);
                plugin.getThemeRegistry().register(theme);
            });
        });

        ThemeCreationRegistry.getInstance().removeByController(this);
    }

    public void handleChatInput(String message) {
        switch (state) {
            case SELECT_NAME -> handleNameInput(message);
            case ENTER_DESCRIPTION -> {
                setDescription(message);
                nextState();
            }
            default -> { /* not currently expecting chat input */ }
        }
    }

    private void handleNameInput(String message) {
        Player player = Bukkit.getPlayer(userId);
        if (message.length() < 3) {
            if (player != null) {
                player.sendMessage(ColorFormat.format("&cName is too short."));
            }
            return;
        }
        if (message.length() > 16) {
            if (player != null) {
                player.sendMessage(ColorFormat.format("&cName is too long."));
            }
            return;
        }
        if (!NAME_PATTERN.matcher(message).matches()) {
            if (player != null) {
                player.sendMessage(ColorFormat.format("&cInvalid theme name."));
            }
            return;
        }

        setName(message);
        nextState();
    }

    private enum ThemeCreationState {
        SELECT_NAME, SELECT_BIOME, SELECT_ICON, ENTER_DESCRIPTION, GENERATING_WORLD, SET_SPAWN, BUILDING
    }
}
