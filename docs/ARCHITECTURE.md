# PocketWorld — Standalone Slime-Format Plugin: Architecture

## 1. Context

PocketWorld is an existing personal/instanced-world plugin (`me.modify.pocketworld`, MC 1.19.2 / Java 17) that lets players create, theme, and share small private worlds. It originally worked only because it hard-depended on the **SlimeWorldManager (SWM)** plugin for world storage. SWM's plugin-era architecture is dead — its successor, **AdvancedSlimePaper (ASP)**, abandoned the plugin model entirely and became a full Paper **server fork**, and the ASP maintainers' own code contains warnings telling anyone still running the old SWM plugin to remove it.

Goal: rebuild PocketWorld as a fully standalone plugin that implements the Hypixel **Slime Region Format (SRF)** itself — no ASP, no SWM, no server fork — while preserving PocketWorld's product intent (personal worlds, themes/templates, invitations, ranks) and fixing the real bugs found in the original implementation.

This required research before any design was possible, because the central question — *"can a plugin alone do what ASP's fork does?"* — has a hard, verifiable answer, not a matter of opinion. That question was investigated three times over the course of this project (§2 at the outset; §15 once a real implementation existed to test against, against Paper 1.21.x; §17 again directly against Paper 26.2, rather than assuming the first result carried over) and got the same answer every time, with progressively harder evidence.

## 2. Research Findings (condensed)

**ASP architecture** — `core`/`api`/`loaders` modules are **100% portable pure-Java** (verified via grep: zero `net.minecraft.*`/CraftBukkit imports). They do byte-level format read/write (`SlimeSerializer`, versioned readers v1_9→v13), an in-memory NBT-backed world model (`SkeletonSlimeWorld`), a from-scratch Anvil-format `.mca` reader/writer (`AnvilWorldReader`), and a storage-agnostic `SlimeLoader` interface (`byte[] readWorld/saveWorld`, file/MySQL/MongoDB/Redis/HTTP backends). None of that needs a fork.

What genuinely **requires** the fork: `aspaper-server`'s `SlimeLevelInstance extends net.minecraft.server.level.ServerLevel`, constructed directly with a custom `LevelStorageAccess` and wired into Paper's internal async chunk system (`ca.spottedleaf.moonrise.patches.chunk_system.*`) via custom `EntityDataController`/`PoiDataController` implementations, using an Access-Transformer file to widen NMS field visibility at build time. **A plugin cannot subclass `ServerLevel` or control which class the server instantiates for a world** — that specific mechanism is fork-only.

**The old plugin-era SWM** (pre-ASP) *did* run live, in-place chunk loading from a plain plugin — via per-Minecraft-version NMS submodules (`slimeworldmanager-nms-v1_8_R3` … `v1_15_R1`) implementing NMS's `IChunkLoader` directly and substituting it into a constructed `CustomWorldServer`. This broke down around 1.17–1.18 when Bukkit's chunk/ticket system changed enough that ASP's own README says the project "had to pivot to a Paper fork." Confirmed independently: **Paper's own maintainers rejected a request for a pluggable region-storage API** (GitHub Discussion #10559), explicitly citing unsustainable maintenance cost and pointing people at forks instead.

**Original conclusion this design was built on**: there is no supported, stable way to make a live Bukkit `World` stream chunks from arbitrary storage without either (a) forking, or (b) reflection into Minecraft server internals that would need real, version-specific engineering and could break across versions. The plan going in was to build (b) for two named version floors (26.2 and 1.21.x) as a hands-on NMS project, with a reflection-free fallback ("Anvil-shadow") always available as a safety net.

**What actually happened (§11)**: (b) was investigated properly once a real implementation existed to test the hypothesis against, using hard evidence (the actual patched server jar's bytecode, and ASP's own Access Transformer file) rather than the design-time reasoning above. The conclusion held, with much sharper detail on *why*: a live custom-chunk-storage bridge needs to be present *during* `ServerLevel`'s own constructor, which only subclassing (i.e. forking) allows. Reflection into an already-constructed world is too late — by then everything downstream has already wired itself to the original objects. So (b) was not pursued further; instead, the plugin's one reflection-free bridge was made significantly faster in the case that actually matters for this product (§7, §11).

**Versions**: Minecraft moved to `YY.D.H` versioning in late 2025. Current release is **Java Edition 26.2 "Chaos Cubed"** (released 2026-06-16); the prior scheme's final release was **1.21.11**. Current Paper (`io.papermc.paper:paper-api:26.2.build.+`) **requires Java 25** to run a 26.2 server.

**Libraries**: `com.github.luben:zstd-jni` (BSD-2-Clause) for compression — same library ASP uses, standard for this ecosystem. `net.kyori:adventure-nbt` (MIT) is a clean, well-tested NBT tree implementation ASP also uses — reusing it (as a dependency, not copying its code) avoids writing/maintaining our own NBT parser, and is shaded since it's not guaranteed present on every Paper build. `com.zaxxer:HikariCP` and `com.mysql:mysql-connector-j` back the MySQL storage/metadata backends; `org.mongodb:mongodb-driver-sync` backs the MongoDB ones.

**Licensing**: ASP is GPLv3, no per-file headers, no dual license. Its *architecture and the SRF byte layout itself* (facts/format, not copyrightable) are fair to learn from; its *code* is not fair to copy or lightly adapt — doing so would create a GPLv3 obligation on our source. **This project is a clean-room re-implementation**: everything here is written from understanding, not from ASP's source.

**Original PocketWorld repo** — solid, reusable UI/domain layer (menus, theme-creation wizard, invitations, ranks) sitting on top of SWM calls that had to be fully replaced. Real bugs found that this rewrite fixes rather than reproduces:
- **Inverted invitation map on Mongo deserialize** (`MongoAdapter.pocketWorldFromDocument`) — invites silently stop resolving to their real recipient after any cache-miss reload.
- `ThemeLoader.deleteWorld()` calls `randomAccessFile.write(null)` → uncaught NPE.
- No mechanism unloads a world when its last player leaves (the code meant to do this was dead/orphaned, by its own comment "way too server intensive" — it had no debounce, so it would have thrashed worlds on every brief disconnect).
- `MySQLConnection` is an empty stub; selecting `mysql.use: true` alone passes config validation but NPEs on first DB call.
- `CommandTest` is an ungated debug command exposing raw inventory/item editing — must not ship.
- Main-thread-blocking synchronous Mongo calls inside `CreatureSpawnEvent` and several menu click handlers.
- Pagination bugs in four list-style menus (`.get(i)` used instead of `.get(index)`, so "next page" never actually advanced) and a bounds-check bug in the outgoing-invitations menu.
- A third-party AnvilGUI dependency for three text-input flows (theme name, world name, invite username) — replaced with an in-house chat-based input mechanism (§6) per explicit preference to avoid that dependency.
- No public API/events for other plugins to integrate with.
- No test suite at all.

## 3. Slime Region Format Primer

Binary layout (Hypixel SRF, as documented in AdvancedSlimePaper's `SLIME_FORMAT`, versions v1–v13): magic `0xB10B` + version byte, world DataVersion int, a flags bitmask (POI chunks / fluid ticks / block ticks present), then a zstd-compressed block of chunk data (per chunk: x/z, per-section light + block-state NBT + biome NBT, heightmaps, optional POI/tick data, tile entities, entities), followed by a separate zstd-compressed "extra" NBT compound reserved for custom/PDC data.

This project's `SlimeReader`/`SlimeWriter` implement only the current format version (`SlimeConstants.CURRENT_VERSION = 13`) — an intentional simplification from the original plan to support the full v1–v13 read history with a stepwise upgrade chain. A file claiming an earlier version (1–12) is recognized and reported via a typed `UnsupportedSlimeVersionException` rather than silently misread; nothing produced by this plugin is ever anything but v13. This was a pragmatic call: nothing in this project's own pipeline (create/import) ever produces an older version, so the upgrade chain's only real use would be reading a v1–v12 file authored by *other* SRF tooling, which was judged not worth the added surface area for a first implementation.

**Key insight for the runtime design**: because SRF chunk NBT is just "the same format as vanilla," and vanilla/Paper servers already carry Mojang's own DataFixerUpper to silently upgrade old-DataVersion world folders on load, the Anvil-shadow bridge (§7) gets cross-Minecraft-version chunk migration **for free** simply by writing technically-valid old-DataVersion Anvil NBT and letting the server's normal load path upgrade it — exactly like opening an old vanilla world. We do not need to reimplement Mojang's block-state/biome migrations ourselves.

## 4. High-Level Architecture

Two Maven modules under one reactor `pom.xml`, so the format/storage engine is independently testable and maintainable:

```
pocketworld/ (parent pom, packaging=pom, compiles to Java 21 bytecode)
├── pocketworld-slime/      -- format + storage engine (NO Bukkit/Paper dependency)
└── pocketworld-plugin/     -- Paper plugin: runtime, domain, UI, commands (depends on pocketworld-slime)
```

Compile target is **Java 21 bytecode**, not 25: Paper 26.2 requires a Java 25 JVM to run, but our other supported floor (Paper 1.21.x) only requires Java 21. A single plugin jar must run on both, and newer JVMs can run older bytecode but not vice versa, so 21 is the target that works everywhere we support.

`pocketworld-slime` has zero Bukkit dependency so it can be unit-tested with plain JUnit and, if ever useful, reused outside this plugin. `pocketworld-plugin` is the only module that touches Bukkit/Paper.

## 5. Module: `pocketworld-slime`

Actual current structure (verified against the source tree, not the original plan — a few packages planned early on, `validation/` and a standalone `clone/`, were never built; corruption is instead reported via typed exceptions at read time, and cloning turned out to need nothing beyond a default method on `WorldLoader` itself):

```
com.pocketworld.slime
├── nbt/            -- NbtIO: thin wrapper around net.kyori:adventure-nbt
├── format/          -- binary container: header, flags, version check
│   ├── SlimeConstants.java          (magic, CURRENT_VERSION = 13, MIN_KNOWN_VERSION = 1)
│   ├── SlimeReader.java / SlimeWriter.java   (top-level read(InputStream)/write(OutputStream, SlimeWorldData))
│   └── SlimeFormatException.java / CorruptedSlimeFileException.java / UnsupportedSlimeVersionException.java
├── model/           -- immutable in-memory world representation
│   └── SlimeWorldData.java, SlimeChunkData.java, SlimeChunkSection.java, SlimeWorldFlag.java
├── anvil/           -- vanilla Anvil (.mca) reader/writer AND the Slime<->vanilla-NBT reshaper - clean-room,
│   │                   used by BOTH the importer/exporter and the Anvil-shadow runtime bridge in pocketworld-plugin
│   ├── RegionFile.java (sector table, 4KiB sectors, zlib/gzip per chunk), ChunkPos.java
│   ├── AnvilWorldReader.java / AnvilWorldWriter.java
│   └── AnvilChunkConverter.java   (structural reshaping only - block-state palettes, biome data, and
│                                     entity NBT all pass through completely opaque and untouched; see §7)
├── compression/     -- SlimeCompression: Zstd(Input|Output)Stream wrappers over zstd-jni
└── storage/         -- WorldLoader (byte[]-level, storage-agnostic) + backends
    ├── WorldLoader.java, UnknownWorldException.java, WorldAlreadyExistsException.java
    └── loader/{file,mysql,mongo}/…   (FileWorldLoader default; MysqlWorldLoader via HikariCP;
                                         MongoWorldLoader via mongodb-driver-sync, GridFS for >16MB blobs)
```

Key interface (storage-agnostic, deliberately small):
```java
public interface WorldLoader {
    boolean exists(String worldId) throws IOException;
    byte[] read(String worldId) throws IOException;
    void write(String worldId, byte[] data) throws IOException;
    void delete(String worldId) throws IOException;
    List<String> list() throws IOException;
    default void cloneWorld(String worldId, WorldLoader targetLoader, String targetWorldId) throws IOException {
        // default: read-then-write; a concrete loader may override with a same-type fast path
        // (e.g. FileWorldLoader does a plain filesystem copy instead of a round trip through memory)
    }
}
```
This is intentionally close to what ASP's `SlimeLoader` looks like from the *outside* (it's the obvious shape for "byte blob store"), but written independently — no shared code.

## 6. Module: `pocketworld-plugin`

Actual current structure. The `nms/`, `compat/`, and per-version bridge packages that the original plan called for do not exist — §2/§11 explain why a live NMS chunk-storage bridge was concluded to be fork-only, so there is exactly one runtime bridge, not a family of them:

```
com.pocketworld.plugin
├── PocketWorldPlugin.java     -- bootstrap: wires storage, the runtime bridge, metadata data source, caches,
│                                  theme registry, listeners, commands, and the public API service
├── api/            -- public service + events for other plugins to integrate with (nothing like this
│   │                  existed in the original codebase)
│   ├── PocketWorldAPI.java (+ PocketWorldAPIImpl), registered via Bukkit's ServicesManager
│   └── event/  PocketWorldCreateEvent, PocketWorldLoadEvent, PocketWorldUnloadEvent,
│               PlayerEnterPocketWorldEvent, PlayerLeavePocketWorldEvent
├── runtime/        -- the live world-lifecycle orchestration (replaces every SlimePlugin call)
│   ├── PocketWorldRuntime.java     (create/load/unload/clone/import/export - orchestrates the bridge + storage;
│   │                                 see §7 for the exact method sequencing and threading contract)
│   ├── WorldProperties.java        (spawn/difficulty/pvp - runtime parameters outside the Slime binary format)
│   └── bridge/
│       ├── WorldRuntimeBridge.java     (the one seam between "a decoded Slime world" and a live Bukkit World;
│       │                                 see §7 for its full method set, including the warm-cache contract)
│       ├── anvil/AnvilShadowBridge.java   (the only bridge: materializes Slime data as a real, temporary
│       │                                    vanilla-format world folder via WorldCreator, with a warm,
│       │                                    in-session cache - see §7/§11)
│       ├── anvil/LevelDatWriter.java   (writes the minimal level.dat that lets Bukkit treat a folder as an
│       │                                 existing world; includes a WorldGenSettings block required by
│       │                                 Paper 1.21.x - see §11)
│       └── BridgeSelector.java     (checks WorldRuntimeBridge.isAvailable() and picks the first that reports
│                                     true - today, always AnvilShadowBridge; the seam stays in place for any
│                                     future bridge that turns out to be genuinely viable)
├── world/          -- domain model (ported + cleaned from the original repo)
│   └── PocketWorld.java, PocketWorldCreator.java, WorldSpawn.java, WorldRank.java,
│       Invitation.java (NEW explicit type, replaces the buggy raw UUID map - see §2)
├── theme/          -- world templates ("theme" naming kept from the original product)
│   └── PocketTheme.java, ThemeRegistry.java, creation/ThemeCreationController.java (+ Registry, SingleBiomeProvider)
├── data/           -- metadata persistence (users/worlds/themes) - independent of the Slime world-blob
│   │                  storage above; a server can keep world files on disk while storing metadata in
│   │                  MySQL, or vice versa
│   ├── DAO.java, Connection.java, DataSource.java   (DataSource picks mongo/mysql/file per config,
│   │                                                   defaulting to file when neither DB is enabled)
│   ├── mongo/, mysql/   -- real backends (MySQL was an unimplemented stub in the original repo - see §2)
│   ├── file/           -- FileConnection/FileDAO: one YAML file per world/user/theme, atomic
│   │                     temp-file-then-move writes - the zero-external-dependency default (NEW; the
│   │                     original repo, and this project through Stage 5, always required a database)
│   └── config/         -- PluginFile/ConfigFile/MessageFile (config.yml/messages.yml wrappers)
├── cache/          -- PocketCache<T> reach-through cache base + WorldCache/UserCache
├── command/, listener/, ui/, util/   -- ported from the original repo (menus, invitations UX, permissions);
│                                        CommandTest removed, commands moved off a reflection-based
│                                        CommandMap-registration hack onto plugin.yml + CommandExecutor,
│                                        listener/WorldAutoUnloadTracker is a real (debounced) replacement
│                                        for the original's dead auto-unload code (see §2, §9)
└── exceptions/     -- DataSourceConnectionException
```

**Why keep the DAO/DataSource seam from the original repo**: it's already a clean boundary (`Connection.getDAO()`) that nothing else needs to change around — the whole `ui/` tree, `cache/`, and `command/` (minus `CommandTest`) don't touch SWM at all and ported over close to unchanged.

## 7. World Lifecycle and the Runtime Bridge Contract

`PocketWorldRuntime` is the one class the rest of the plugin (commands, menus, listeners) needs to call for the whole world lifecycle; everything about the format and the runtime bridge is an implementation detail behind it. Its methods are each documented as either pure I/O (safe off the main thread) or main-thread-only (touch live Bukkit world state) - callers do their own async-then-sync scheduling around these, which is what `PocketWorld.load()`/`unload()`/`PocketWorldCreator` actually do.

**`WorldRuntimeBridge`** (the seam a runtime bridge implements):
```java
public interface WorldRuntimeBridge {
    String name();
    boolean isAvailable();
    OptionalInt cachedDataVersion(String worldName);          // warm-cache read side - see below
    void prepare(SlimeWorldData data, String worldName) throws IOException;      // pure I/O
    World activate(String worldName, int dataVersion, WorldProperties properties) throws IOException;  // main thread
    SlimeWorldData extract(World world) throws IOException;  // main thread
    void afterUnload(String worldName, Path worldFolder, boolean retain, int dataVersion) throws IOException;
    void evictCache(String worldName) throws IOException;     // permanent delete, even if never loaded this session
}
```

**Create** (from theme): `WorldLoader.read(themeId)` (theme runtime's own storage) → `cloneInto` copies the raw bytes directly into the world runtime's storage under the new world id (no decode/re-encode - both runtimes speak the same Slime binary format) → `prepareLoad`/`activate` as below.

**Load**: `PocketWorldRuntime.prepareLoad(worldId)` first asks the bridge `cachedDataVersion(worldId)` - if present, the expensive decode-and-rewrite is skipped entirely and that value is used directly; otherwise it reads and decodes the stored Slime bytes and calls `bridge.prepare(data, worldId)`. Either way it returns the world's data version, which `PocketWorldRuntime.activate(worldId, dataVersion, properties)` (main thread) then passes to `bridge.activate`.

**Unload/Save**: `PocketWorldRuntime.unloadSync(world, worldId, save)` (main thread) - if `save`, calls `bridge.extract(world)` to pull the live world's current state into `SlimeWorldData`; either way calls `Bukkit.unloadWorld()`, then `bridge.afterUnload(worldId, worldFolder, retain, dataVersion)` where `retain` is true only on a successful save. Extracted data is *not* written to storage yet - the caller passes it to `PocketWorldRuntime.persist(worldId, data)` separately, off the main thread.

**The warm cache** (§11 has the full empirical story): `AnvilShadowBridge` does not delete a world's on-disk Anvil folder after a saving unload the way the original design did - it keeps it, and `cachedDataVersion` reports it as usable next time if the folder's still there in the expected shape. This is self-limited to one server session (an in-memory map, never trusted across a restart) and bounded by a 30-minute disuse TTL, and it only ever retains when doing so is actually safe (see §11 for the one real bug this caught: a naive version of this unconditionally left behind a copy of the data at Paper 26.2's post-migration location, which then broke the *next* migration attempt).

**Clone**: `PocketWorldRuntime.cloneWorld`/`cloneInto` - pure storage-layer operations; `WorldLoader.cloneWorld`'s default implementation streams bytes through memory, and `FileWorldLoader` overrides it with a plain filesystem copy when source and target are both file-backed.

**Import/Export**: `PocketWorldRuntime.importWorld`/`exportWorld` use `AnvilWorldReader`/`AnvilWorldWriter` directly, no bridge involved - reachable via `/pocketworldadmin` (not yet wired to a subcommand as of this writing; the runtime methods exist and are tested, the command surface is still to come).

## 8. Version Compatibility Strategy

There is exactly one runtime bridge (`AnvilShadowBridge`), and it runs identically on every supported Paper version - §2/§11 cover why a version-specific NMS bridge was investigated and concluded not to be safely buildable without forking. `BridgeSelector` still exists as the seam for that to change (it iterates a list of candidate bridges and picks the first whose `isAvailable()` is true), so a future bridge - version-specific or not - could be added without touching any calling code, but today the list has one entry.

- Minimum documented support: **Paper 26.2 (Java 25)** and **Paper 1.21.x (Java 21)**, both running the same Anvil-shadow bridge. Any other Paper version in between is expected to work the same way, though only these two floors have been empirically verified end-to-end (§11).
- The two floors are *not* equally fast for the same reason `cachedDataVersion` exists: Paper 26.2's own world-creation migration (`LegacyCraftBukkitWorldMigration`) relocates a newly created world's files to a different final location every single time, which means the warm cache's freshness check (which only trusts the pre-migration path) correctly and safely never reports a hit there - every load on 26.2 pays the full decode-and-rewrite cost. On 1.21.x, where no such migration happens, the cache reliably hits and a revisit is close to free (confirmed: 418ms -> 0ms in the same session, region folder untouched). See `docs/COMPATIBILITY.md` for the user-facing version table.

## 9. Migration Strategy (original PocketWorld code)

Not a line-by-line port — a rewrite of the SWM-facing slice with the UI/domain layer carried over deliberately:
- **Carried over near-unchanged**: `ui/` menu tree, `cache/`, most of `command/` (drop `CommandTest`), `util/`, `theme/` UI/state-machine (only its `slime.*` calls repointed at `PocketWorldRuntime`).
- **Rewritten fully**: everything that called `SlimePlugin`/`SlimeLoader`/`SlimeWorld` — `PocketWorld.load/unload/delete`, `PocketWorldCreator`, `ThemeCreationController`'s world-generation calls, `SlimeHook`/`PocketHook` (deleted — no external plugin dependency anymore), `MongoLoader`/`ThemeLoader` (replaced by `pocketworld-slime`'s storage backends).
- **Fixed in place**: invitation model (`Invitation{sender, recipient, timestamp}`, fixing the inversion bug), world-empty auto-unload (`listener/WorldAutoUnloadTracker`, a real debounced replacement for the dead orphaned method), MySQL support (real HikariCP-backed implementation, for both world-blob storage and metadata), `plugin.yml` permissions block, the debug command removed, three AnvilGUI-based text-input flows replaced with an in-house `ChatInputRegistry`.
- **New, not ported**: `api/` package (events + service), `data/file/` (zero-dependency YAML metadata storage - the original always required Mongo or MySQL even though world *data* itself was file-based by default).

## 10. Testing Strategy

What's actually in place, as opposed to the original plan (which anticipated MockBukkit-based integration tests for command/listener/menu behavior - that direction wasn't taken; plain JUnit plus real-server empirical verification covered what was needed instead):
- **`pocketworld-slime`** (pure JUnit 5, no Bukkit): round-trip tests per format version, corruption tests (truncated/flipped-byte files fail cleanly with a typed exception), Anvil reader/writer round-trip against real small vanilla region-file fixtures, loader backend tests (`FileWorldLoader` against a temp dir; `MysqlWorldLoader`/`MongoWorldLoader` against Testcontainers, which skip rather than fail when no local Docker daemon is reachable).
- **`pocketworld-plugin`**: plain-JUnit unit tests for logic that doesn't need a live Bukkit server (`ChatInputRegistry`, `WorldSpawn`, `Invitation`, `WorldRank`, `PocketUtils`, and a full YAML round-trip test for `FileDAO`).
- **Everything that genuinely depends on live Bukkit/world-creation behavior** (the runtime bridge's prepare/activate/extract/unload cycle, the warm cache, `keepSpawnLoaded`, teleport timing) is verified by actually running a real Paper server and driving `PocketWorldRuntime` directly via a temporary, throwaway admin command (never committed) - the same approach used to find and fix the migration-retention bug and the 1.21.x level.dat gap in §11. This was a deliberate choice, not a gap: this class of behavior is exactly what a mock can't usefully stand in for.

## 11. Risks & Technical Limitations

- **A live custom-chunk-storage bridge is fork-only** — investigated twice (§2 at design time, then again with hard evidence once a real implementation existed to test against - see the Stage 6 findings below) and concluded both times that reflection cannot reach a hook point early enough to matter; only subclassing `ServerLevel` during its own construction can. This is treated as settled, not an open risk to keep managing.
- **Anvil-shadow performance on Paper 26.2 specifically**: every load pays the full decode-and-rewrite cost, because the platform's own world-creation migration defeats the warm cache's freshness check every time (§8). This is an accepted, understood trade-off given the alternative is forking, not a bug - and it's the reason 1.21.x and 26.2 are documented as behaving differently despite running identical code.
- **zstd-jni is a native library** — must ship/shade correct natives for the platforms supported; deliberately excluded from `pocketworld-plugin`'s shade relocations (§14) since relocating its Java package breaks its native bindings.
- **Java 25 requirement** (for the 26.2 floor) drops compatibility with any host still on older JVMs; this is dictated by current Paper itself, not a choice made here.
- **GPLv3 exposure**: because this design was informed by studying ASP's source, all resulting `pocketworld-slime`/`pocketworld-plugin` code is written fresh (no copy-paste, no "translate variable names" adaptation) to avoid inheriting a GPLv3 obligation.
- **The warm cache is deliberately conservative**: it only ever trusts its own record when the exact on-disk state it expects is still physically present, and never across a server restart. Any doubt collapses to a full rebuild rather than a risk of serving stale data - the one bug found during development (§14) was caused by *not* being conservative enough (retaining a folder that a platform-specific migration had already emptied), and the fix tightened the retain condition rather than adding a bypass.

## 12. Staged Implementation Roadmap (historical)

Each stage compiled, was tested, and was explained before moving on. Kept here as a record of how the project was actually built, including where the plan changed once real evidence came in (Stages 6-7).

0. **Scaffold**: reactor `pom.xml`, both modules, empty-but-valid plugin jar. Docs (`README.md`, `docs/COMPATIBILITY.md`, this file). ✅
1. **Format engine core**: `nbt`, `format`, `model`, `compression`. Unit tests: round-trip + corruption handling. ✅
2. **Anvil layer**: `anvil/RegionFile`, `AnvilWorldReader`/`Writer`. Tests against small real vanilla region-file fixtures. ✅
3. **Storage layer**: `WorldLoader` + File/MySQL/Mongo backends. Tests (Testcontainers for MySQL/Mongo). ✅
4. **Runtime skeleton + Anvil-shadow bridge**: `PocketWorldRuntime`, `WorldRuntimeBridge`, `AnvilShadowBridge`, `BridgeSelector`. Made the plugin fully functional end-to-end (create/load/unload/clone/import/export) with zero NMS code — the real, shippable milestone the rest of the project builds on. ✅
5. **PocketWorld domain/API/UI port**: domain model, `data/` (bug-fixed DAO, real MySQL), theme system, UI/command/listener port, new public `api/` package + events, bug fixes (invitations, auto-unload, permissions, remove debug command, AnvilGUI removal). Followed by a standalone addition: `data/file/` zero-dependency metadata storage. ✅
6. **NMS bridge spike — 1.21.x**: originally planned as "build a reflection-based bridge if viable." Actual outcome: inspected the real patched 1.21.11 server jar's bytecode plus ASP's own Access Transformer, concluded a live bridge needs to be present during `ServerLevel` construction (fork-only, not reflection-reachable after the fact) — see §11. Pivoted to a warm-cache redesign of `AnvilShadowBridge` instead, which does deliver a real, measured speedup on this floor (§14/§15/§16). Also fixed a level.dat gap this same testing surfaced, blocking 1.21.x world activation outright. ✅
7. **NMS bridge spike — 26.2**: re-investigated with the same bytecode-level rigor as Stage 6 rather than assumed from it. Same conclusion, now directly confirmed against 26.2's own patched server jar rather than inferred from 1.21.11's — see §17. ✅
8. **Polish**: corruption/repair tooling, format migration command, compatibility docs finalized, permission defaults review. Not yet started.

## 13. Verification Approach

- Stages 1–3 verified with `mvn test` in `pocketworld-slime` alone (no server needed).
- Stage 4 onward verified by running real Paper servers locally and exercising create/load/unload/clone/import - initially via in-game commands, later (once the product surface was large enough that RCON/console couldn't drive it directly) via a temporary, throwaway admin command that called `PocketWorldRuntime` directly and logged each step, always removed before the corresponding work was committed.
- Stage 6 additionally verified against a real Paper 1.21.11 server instance, comparing warm-cache load timing against Paper 26.2's to confirm the expected speedup was real and not just theoretical (§16).

## 14. Empirical Findings — Stage 4

Several things below could not have been determined by reasoning about the documented Slime format
or general Minecraft knowledge alone; they were only found by actually running a real Paper 26.2
server, generating a world, and inspecting its files with the Stage 1/2 code itself.

- **No explicit per-section Y field in the documented Slime format** - resolved by confirming real
  vanilla sections carry an explicit `Y` byte tag as a sibling of `block_states`/`biomes`; the Slime
  format's "block states nbt compound" field is understood to fold `Y` in alongside `block_states`
  (`AnvilChunkConverter` implements this mapping).
- **Entities live in a separate per-region file set** (`entities/*.mca`, keyed by
  `Position`/`DataVersion`/`Entities`), not embedded in the main chunk - confirmed by summoning real
  entities, forcing a save, and observing where they actually landed on disk (a transient `entities`
  key briefly seen in an *un-generated* chunk was generation-pipeline scratch state, not the
  persisted form - a wrong first guess corrected before it became a design decision).
- **Minecraft 26.2 restructured on-disk world storage**: each dimension gets its own
  `dimensions/<namespace>/<dimension>/{region,entities,data,paper-world.yml}` folder, and what used
  to be one `level.dat` is now split across many small per-dimension `.dat` files (world-gen
  settings, game rules, weather, raids, scheduled events, chunk tickets, world border) plus Paper's
  own `data/paper/*.dat`. None of this is preserved or reconstructed by this plugin - `WorldCreator`'s
  own parameters are relied on to synthesize sensible defaults, the same way chunk-level generation
  bookkeeping (`Status`, `structures`, `PostProcessing`) is deliberately not preserved either.
- **Secondary (non-default) worlds nest under the *primary* world's own `dimensions/` folder**,
  keyed by the secondary world's name (`<primary>/dimensions/minecraft/<name>/`) - not their own
  independent top-level folder the way classic Bukkit multi-world always worked. `AnvilShadowBridge`
  does not try to predict or construct this path: it writes the classic `<container>/<name>/region`
  layout and lets Paper's own `LegacyCraftBukkitWorldMigration` relocate it, which was confirmed (by
  running it, not assumed) to fully relocate the data with no leftover. Reading a *live* world back
  out uses `World#getWorldFolder()` directly, which reliably resolves to wherever the data actually
  lives regardless of internal layout - Bukkit already abstracts this away once the world exists;
  the only genuinely hard part is predicting a path *before* the world exists, which is exactly what
  relying on the migration path sidesteps. This same migration behavior is what makes the warm cache
  (§7, §16) unable to ever hit on 26.2, and what caused the retention bug described in §16.
- **Shading `zstd-jni` breaks it.** Its native library's exported JNI symbols are hardcoded to the
  `com.github.luben.zstd` class names, so relocating the Java package causes `UnsatisfiedLinkError`
  at the first native call - only surfaced by actually running the shaded jar on a real server, not
  by anything `mvn package` checks. `zstd-jni` is deliberately excluded from `pocketworld-plugin`'s
  shade relocations.

## 15. Empirical Findings — Stage 6, NMS Bridge Spike (Paper 1.21.x)

**Conclusion on a full custom-storage NMS bridge**: not achievable via reflection alone on 1.21.11,
confirmed by inspecting the real patched server jar's bytecode (not inferred). `ChunkMap` extends
`SimpleRegionStorage` and implements Moonrise's `ChunkSystemChunkMap`; real chunk I/O bottoms out in
`ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO$RegionDataController`, a
private async scheduler with no plugin-facing seam. AdvancedSlimePaper's own Access Transformer
(`build-data/aspaper.at`) confirms *why* reflection can't substitute for this even after the fact:
it needs `protected-f` (remove `final`) on `ServerLevel.chunkTaskScheduler`/`entityDataController`/
`poiDataController`, because those fields are read and handed off to other objects **during
`ServerLevel`'s own constructor** - by the time a live world exists to reflect into, everything
downstream has already wired itself to the originals. The only way in is to be present during
construction (subclass `ServerLevel`), which is exactly the fork ASP already is.

This directly motivated the warm-cache redesign (§7, §16): if the format-decoding side was never
the actual bottleneck, a genuine speedup was still reachable without touching NMS at all.

## 16. Empirical Findings — Stage 6, Warm-Cache Redesign and the 1.21.x level.dat Fix

**What was built**: a "warm cache" redesign of `AnvilShadowBridge` and the `WorldRuntimeBridge`
contract (`cachedDataVersion`/`afterUnload(retain)`/`evictCache`), driven by the observation that the
actual cost center wasn't chunk-format decoding - it was the full decode-Slime-bytes-then-rewrite-a-
fresh-Anvil-folder round trip happening on *every single* load/unload, even when a world was
revisited moments after it was last used. A world's on-disk folder is now kept after a saving unload
instead of deleted, and reused directly (skipping decode and rewrite entirely) if nothing's
invalidated it - self-limited to one server session (never trusted across a restart) and bounded by
a 30-minute disuse TTL.

Also applied: `keepSpawnLoaded(TriState.FALSE)` on every pocket world (vanilla forces an ~11x11
chunk area permanently loaded around spawn regardless of world size - wasteful for a small, bounded
instanced world), and removal of a 20-tick artificial delay before teleporting a player into a
newly loaded/created world (`Bukkit.createWorld()` already synchronously prepares the spawn area
before returning, confirmed via server logs - the delay was vestigial from the original codebase).

**A bug caught by empirical testing on Paper 26.2**: the first version of this design retained the
folder unconditionally, which left behind a copy of the data at its *migrated* location
(`LegacyCraftBukkitWorldMigration` relocates it into the primary world's own `dimensions/` folder on
26.2 - §14), which then made the *next* migration attempt fail outright because its destination
already existed. Fixed by only ever retaining when the live world's data folder path equals the
classic pre-migration path (i.e. no migration happened this activate) - otherwise always fully clean
up regardless of the `retain` flag. Net effect on 26.2: the warm cache safely never actually hits
(its own freshness check requires the classic path to still hold the data, which migration always
defeats), but every cycle remains fully correct.

**A separate, pre-existing bug surfaced by testing on Paper 1.21.11**: activating a
bridge-materialized world failed with `IllegalStateException: No key dimensions in MapLike[{}]; No
key seed in MapLike[{}]` - `LevelDatWriter` (built and only ever verified against 26.2 in Stage 4)
did not write a `WorldGenSettings` compound at all, and 1.21.11's world-loading codec requires one
where 26.2's apparently doesn't. This meant the documented "Paper 1.21.x" support floor had compiled
successfully but had never actually been exercised end-to-end against a real 1.21.x server until
this testing. **Fixed**: `LevelDatWriter` now writes a `WorldGenSettings` block that's a structural
copy of what a real Paper 1.21.11 server writes for an ordinary secondary world (confirmed by
creating one and inspecting its level.dat directly, via a temporary debug command, not guessed) -
`generator.settings` turned out to be just a string reference to a built-in noise-settings preset
name (`"minecraft:overworld"` etc.), not an inline definition, so this needed no per-world generation
parameters of our own. The seed/generator are never actually exercised in normal operation, since
every chunk a pocket world's border lets a player reach is one the bridge already wrote explicitly -
a fixed placeholder seed is used.

**Confirmed end-to-end on both real servers** (the full create→unload→reload→unload→delete cycle,
via a temporary throwaway admin command, never committed):
- **Paper 26.2**: every step succeeds; the warm cache safely never hits, exactly as predicted (second
  `prepareLoad()` took ~446ms, about the same as the first, with the region folder's mtime changed -
  a full rebuild each time, correctly).
- **Paper 1.21.11**: every step succeeds, including the fixed level.dat - and the warm cache's
  expected win is real: second `prepareLoad()` dropped from **418ms to 0ms**, with the region
  folder's mtime **unchanged**, confirming a genuine cache hit rather than a coincidentally-fast
  rebuild.

## 17. Empirical Findings — Stage 7, NMS Bridge Spike (Paper 26.2)

Stage 6's conclusion (§15) was reached against Paper 1.21.11 specifically. Rather than assume it
carries over to 26.2 just because both run on Moonrise, Stage 7 repeated the same bytecode-level
inspection directly against 26.2's own patched server jar (the same one already used for every other
piece of empirical verification in this project).

**Same conclusion, now directly confirmed rather than inferred**: `SimpleRegionStorage` implements
`ChunkSystemSimpleRegionStorage` and `RegionFileStorage` implements `ChunkSystemRegionFileStorage` -
the identical Moonrise-patched interfaces found on 1.21.11, wrapping the same private `storage`/
`folder` fields. `ServerLevel` on 26.2 declares the exact three fields ASP's Access Transformer
targets - `entityDataController`, `poiDataController`, `chunkTaskScheduler` - as `private final`,
same as 1.21.11. (The ASP research clone itself targets Minecraft 26.1.2, not 26.2 exactly, so its
AT file was corroborating evidence rather than proof for this specific version - the bytecode
inspection is what actually confirms it for 26.2.) A live custom-chunk-storage bridge would need to
intervene during `ServerLevel`'s own constructor on 26.2 for exactly the same reason it would on
1.21.x - reflection into an already-constructed world is structurally too late either way, not a
matter of 26.2-specific hardening.

No code changes resulted from this stage - it was a verification pass, and the existing warm-cache
design already documents 26.2's behavior correctly (§8, §16): the cache never hits there because the
platform's own world-creation migration relocates the data every time, independent of anything to do
with the NMS question this stage answers.

## 18. Empirical Findings — Capacity Investigation and Concurrency Hardening

A capacity question ("would 100 simultaneous PocketWorld creations be safe?") drove a round of load
testing against a real Paper 26.2 server, which surfaced three real, previously-unknown issues -
each confirmed empirically, not assumed, before being fixed:

**Brand-new-world spawn search (fixed).** Vanilla's own first-creation spawn search runs
unconditionally inside `Bukkit.createWorld()` and touches (and permanently persists) a large fixed
radius of empty void chunks around origin - confirmed independent of `keepSpawnLoaded` or how soon
the world border is set afterward. The only thing that avoided it was the world already declaring
itself initialized with a known spawn, via `level.dat`, before `Bukkit.createWorld()` ever ran.
`AnvilShadowBridge.activate()` already did this for every regular PocketWorld create/load;
`ThemeCreationController.generateEditorWorld()` did not, so every theme's first-ever creation was
silently baking ~1000 extra empty chunks into its stored data, which then propagated (via the
byte-level clone `PocketWorldCreator` uses) into every PocketWorld ever made from that theme.

**First-ever chunk touch in virgin territory (fixed, two ways).** A second, deeper issue: even a bare
chunk *load* (no block placement, independent of block opacity or whether Bukkit physics is applied)
in a brand-new, never-touched world forces Paper's own multi-stage chunk-generation pipeline through
a wide-radius pass - almost certainly structure-reference checks needing a neighbor radius, not
anything this plugin's bridge code does. Measured at up to several hundred extra chunks and, when
triggered synchronously (a live block placement, exactly what theme editor-world creation used to do
immediately after `Bukkit.createWorld()`), 1.2-7.8 seconds of complete main-thread blocking - far
worse than initial capacity estimates, which never happened to exercise a genuinely virgin world.
Fixed two ways: `WorldRuntimeBridge.extractUnloaded` now takes a `ChunkBounds` (captured from the
live world's border before it's unloaded, since the border is unrecoverable afterward) and drops any
on-disk chunk outside it regardless of why it exists - a border already stops players from ever
reaching those chunks, so persisting them was always dead weight. Separately, confirmed empirically
that pre-warming a chunk via `World#getChunkAtAsync()` *before* any synchronous touch moves the whole
cascade onto a background thread (tick-heartbeat gaps dropped from ~1500ms to ~100ms across repeated
runs); `generateEditorWorld()` now pre-warms its origin chunk this way before placing the spawn
platform or teleporting the theme creator in.

**`Bukkit.createWorld()`/`unloadWorld()` have no async alternative (confirmed, and designed around).**
Calling `Bukkit.createWorld()` off the main thread throws `IllegalStateException` ("WorldInitEvent
may only be triggered synchronously") - a hard platform restriction, not convention, and not
something reflection should try to route around. Since a burst of simultaneous creation requests
finishing their async prepare work around the same moment could still pile multiple `activate()`
calls into the same tick, `PocketWorldCreationQueue` (`runtime/PocketWorldCreationQueue.java`)
serializes creation server-wide - only one in flight at a time, everyone else queued and told their
position. This doesn't reduce any single creation's cost; it caps the *worst case* to one world's
cost no matter how many requests land at once.

**Verification**: with all three fixes in place, 100 simultaneous creation requests were fired
against the real, unmodified production code (`PocketWorldCreator` + `PocketWorldCreationQueue`) on a
real Paper 26.2 server, with a per-tick heartbeat running throughout to directly measure
responsiveness rather than infer it. Result: 21.4 seconds total for the whole queue to drain, but as
~100 individual 60-280ms hitches (one per creation) with zero gaps over 500ms - the server stayed
responsive the entire time rather than freezing continuously. The cost of a deep queue is wait time
for the player at the back of it, not server health.

## 19. Stage 8 — Polish

**Corruption handling.** The format reader has thrown clean, typed exceptions on malformed data
since Stage 1 (`CorruptedSlimeFileException`, `UnsupportedSlimeVersionException`, both
`SlimeFormatException` subtypes) - but a corrupted stored world previously only ever surfaced as a
caught-and-logged `IOException` with no distinction from a transient I/O failure, and no feedback to
the affected player at all. `PocketWorld.load()` and `PocketWorldCreator`'s creation flow now catch
`SlimeFormatException` specifically, logging a clearly-actionable admin message ("needs manual
recovery, e.g. from a backup") and sending the player a real error message instead of silence.
`PocketWorldRuntime.validate(worldId)` and `.list()` expose the same decode step every load/create
already runs, standalone - pure I/O, no bridge or live Bukkit state involved, so a world can be
checked for corruption proactively rather than only discovered by a player's failed load.

**Migration tooling.** `PocketWorldRuntime.importWorld()`/`.exportWorld()` (built in Stage 4) never
had a command surface. `/pocketworldadmin import <folder> <worldId> [dataVersion]` and
`/pocketworldadmin export <worldId> <folder>` expose them directly, alongside
`/pocketworldadmin validate <worldId|all>` for the corruption check above.

**Permissions.** New admin subcommands share the existing `pocketworld.command.admin` node rather
than fragmenting into per-subcommand permissions - consistent with how `reload` was already gated,
and there's exactly one admin trust tier in this plugin's design. Added a `pocketworld.*` wildcard
(with explicit `children`) for permission-plugin convenience, since server owners commonly grant
staff a single node rather than enumerating four.

## 20. Spigot Compatibility and the Real Cost of Creating a World

**Spigot compatibility.** The plugin still compiles against `paper-api`, deliberately - Paper's API
is a strict superset of Spigot's (Paper only ever adds to it, never removes), so a jar that never
calls a Paper-exclusive symbol runs correctly on both platforms from that same compile target.
Switching the Maven dependency to `spigot-api` instead would require running BuildTools.jar
locally/in CI (Spigot doesn't publish it to any public repo) for no runtime benefit over the
discipline-based approach. A full-tree audit found seven Paper-only call sites, all fixed the same
way - swapped for the plain Bukkit/Spigot equivalent that also works unmodified on Paper:
`io.papermc.paper.event.player.AsyncChatEvent` → `org.bukkit.event.player.AsyncPlayerChatEvent`
(`ChatInputListener`, `ThemeCreationListener` - both now listen at `EventPriority.LOWEST` so this
plugin's cancellation is decided before any other plugin's chat formatter/renderer sees the event,
which is what actually determines whether cancelling the legacy event suppresses the message
reliably); `Player#sendActionBar(Component)` → `player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
...)` (bungeecord-chat API, bundled in spigot-api itself, `MessageReader`); `WorldCreator#keepSpawnLoaded
(TriState)` → `World#setKeepSpawnInMemory(boolean)` called after creation instead of chained on the
creator (`AnvilShadowBridge`, `ThemeCreationController`); `JavaPlugin#getPluginMeta()` →
`getDescription().getVersion()`. The seventh, `World#getChunkAtAsync`, is handled differently - see
`ChunkPrewarmer` below, since it turned out to be the more important lever, not just a Spigot-safety
swap.

**A slot pool was built, measured, and removed.** The first attempt at reducing the "new pocket
world" freeze was a pool of pre-warmed, empty world folders that a real creation could reuse instead
of paying a virgin folder's first-touch cost - reasoned from a benchmark showing a fresh folder costs
~4.8s versus ~0.2s to reuse an already-touched one (even after renaming it to a name Bukkit had never
seen this session, confirmed empirically, see prior revision of this section for the raw numbers).
It was fully implemented (`AnvilSlotPool`) and confirmed working live in production. It was then
**removed**, for two reasons surfaced by directly questioning its value rather than just its
mechanism:

1. It didn't reduce the server's total freeze-seconds, only *whose* action triggered it - every
   consumed slot queues a same-cost replenishment that still freezes the whole server, just decoupled
   in time from the player who benefited. The actual win (bursts of creations absorbing pre-paid
   "credit" instead of queueing full-price) was real but much narrower than "eliminates the freeze."
2. That ~4.8s benchmark turned out to measure a **worst case that doesn't represent real creation**,
   not the typical cost - see below. Once the actual cost was understood, the pool solved a problem
   several times larger than the one that actually exists.

**What the ~4.8s benchmark was actually measuring.** `AnvilShadowBridge.activate()` already writes
`level.dat` with a known spawn via `LevelDatWriter` *before* calling `Bukkit.createWorld()` - an
older fix (§18) for vanilla's spawn search, which runs unconditionally on a virgin folder and touches
a large, fixed radius of chunks looking for solid ground. That prior benchmark never included this
pre-seed step, so it measured the pathological case: a **100% void generator** (no solid ground
*anywhere*) with **no known spawn**, forcing the search to hunt indefinitely. Testing the real,
already-existing mechanism directly (not simulated - the actual `LevelDatWriter.write()` production
code, called before `Bukkit.createWorld()`) on the same test server:

| | createWorld | first chunk touch | Total |
|---|---|---|---|
| Void generator, no known spawn (the old benchmark) | 4770ms | 0ms | **~4.8s** |
| Void generator, `level.dat` pre-seeded (the real, already-existing mechanism) | 86ms | 284ms | **~370ms** |
| Pre-seeded + first touch moved async (`getChunkAtAsync`) | 81ms | *(off main thread)* | **~81ms blocking** |

Real pocket-world and theme-editor-world creation were already landing around ~370-580ms before any
of this session's work, not ~4.8s - the multi-second number was an artifact of a benchmark that
didn't reflect how the bridge actually creates worlds. This matches §18's own conclusion about the
spawn search being the dominant cost; it just hadn't been re-checked against the fix already in place.

**`ChunkPrewarmer`** (`util/ChunkPrewarmer.java`) captures the remaining, real opportunity: the
~280-530ms first-chunk-touch cost above still happens synchronously today, implicitly, whenever the
creating/loading player is teleported in (a teleport forces its target chunk to load if it isn't
already). Explicitly pre-warming that chunk *before* the teleport, via Paper's `getChunkAtAsync`,
moves that cost off the main thread entirely rather than just shrinking it - dropping the main-thread
blocking time to just `createWorld()` itself (~50-100ms). Since `getChunkAtAsync` doesn't exist on
Spigot, `ChunkPrewarmer` resolves it once via `MethodHandles` at class-load time (never a direct
compiled call, which would throw `NoSuchMethodError` the moment this class loaded on a Spigot server)
and transparently falls back to a plain synchronous `getChunkAt` when absent - callers
(`PocketWorldCreator`, `PocketWorld.load()`, `ThemeCreationController`) don't need to know which path
ran; `onReady` always fires exactly once, always back on the main thread. Net effect: **Paper servers
get the full async benefit (~80ms blocking); Spigot servers still get the ~370-580ms pre-seed benefit
over the old un-pre-seeded cost, just without the extra async shrink** - worth stating plainly in any
public listing, since it's a genuine, honest platform difference rather than a marketing rounding.

## 21. Spigot-Only World-Creation Crash: `world_gen_settings.dat`

Reported live from a real Spigot 26.2 server: creating a theme's editor world threw
`IllegalStateException: Overworld settings missing` out of `Bukkit.createWorld()`, preceded by
`Unable to read or access the world gen settings file! ... data/minecraft/world_gen_settings.dat`.
Never seen on Paper 26.2 despite extensive testing there this session.

Root cause, confirmed empirically (not guessed): on this project's current Minecraft floor, a
non-primary world's dimension-generator settings (seed, generator type, structure/feature flags)
are read from a separate `data/minecraft/world_gen_settings.dat` file, not from `level.dat`'s
embedded `WorldGenSettings` tag - confirmed by letting a real server generate and save an ordinary
secondary world, then inspecting the result directly. `LevelDatWriter` only ever wrote `level.dat`;
this bridge-materialized world folders never had this file at all. Paper 26.2 tolerates its absence
(falls back silently to defaults); Spigot 26.2 does not - its fallback path itself throws instead
of recovering. This is exactly the same class of platform-specific loading quirk §16 already found
with the warm-cache path, just in a different corner of world creation.

A second, independent bug surfaced while building the real reference file for comparison:
`LevelDatWriter`'s `WorldGenSettings` compound used the field name `generate_features`; the actual
schema (confirmed against the real file) uses `generate_structures`. Both bugs are fixed together in
`LevelDatWriter.writeWorldGenSettingsFile` - see its class doc. Since `AnvilShadowBridge.activate()`
and `ThemeCreationController.generateEditorWorld()` both call the same `LevelDatWriter.write()`, this
one fix covers both regular pocket-world creation and theme editor-world creation - the live crash
was only ever reported for the latter, but the missing file affected both identically.

**Verification limits, stated plainly**: confirmed on Paper that the file is now written with the
correct structure (byte-for-byte matching a real reference file) and that `Bukkit.createWorld()` no
longer logs the "unable to read" warning for it. The actual Spigot-specific crash could not be
reproduced or re-tested directly - no Spigot server was available in this environment (see §20's
own verification-limits note). This fix is well-founded (it directly addresses a file confirmed
missing, with a structure confirmed correct against real Minecraft output) but needs confirmation
on a real Spigot server before being considered fully verified.

## 22. Spigot-Only Empty-Pocket-World Bug: Region Data at the Wrong Path

Reported live, again from a real Spigot 26.2 server, once §21's crash fix let theme creation
actually complete: every pocket world created from a theme came out as an empty void - the
admin's built content never made it in, even though the theme itself extracted and stored
correctly (confirmed by the user's own end-to-end test: fresh theme, tried twice, both pocket
worlds empty).

Root cause, confirmed by direct inspection of a real Spigot 26.2 server's own files (not
guessed): `AnvilShadowBridge` writes/reads chunk data at the classic `<world>/region`,
`<world>/entities` layout, relying on Paper's own migration to relocate it into whatever internal
structure Paper actually uses (§ "On-disk layout" above). Comparing a real Spigot-generated world
(the theme's own editor world, built via ordinary gameplay with no involvement from this bridge)
against a real Spigot pocket-world folder confirmed: Spigot performs no such relocation at all,
and a NORMAL-environment world's own data instead lives at the vanilla-native
`dimensions/minecraft/overworld/region` path from the moment it's created. Writing to the classic
path on Spigot leaves that data orphaned and silently unread: `Bukkit.createWorld()` finds nothing
at the path it actually looks at and generates fresh, empty content there via `VoidGenerator`
instead - explaining both symptoms the user reported at once (an empty world, and a ~3.3s creation
time matching the "virgin folder" cost §20 already measured, since the freshly-generated content
still pays that cost even though it's pointless output).

Confirmed this really is a platform difference, not a version-wide one, by testing directly on
Paper: writing the *same* pre-existing region data to *both* the classic and modern paths
simultaneously **breaks Paper** (`Failed to migrate legacy world ...` - Paper's own migration
trips over finding its relocation target already occupied). So the fix has to pick one path per
platform, not write both defensively. `AnvilShadowBridge.regionFolder()`/`entitiesFolder()` now
branch on `ChunkPrewarmer.isAsyncAvailable()` (Paper's own already-proven presence check, reused
rather than adding a second detection mechanism) to choose the classic path on Paper or the
modern nested path on Spigot, consistently across `prepare()`, `cachedDataVersion()`, and
`extractUnloaded()`.

**Verification limits, stated plainly**: re-ran the exact real end-to-end pipeline test from §21
against this fix on Paper - full round trip still works, still writes the classic path, zero
regression. The Spigot-side fix itself rests on strong, directly-observed evidence (a real
Spigot-generated folder's actual layout) but - same limitation as §20 and §21 - could not be
tested by actually running it on a Spigot server in this environment. Needs the user's
confirmation before being considered fully verified.
