# PocketWorld — Standalone Slime-Format Plugin: Technical Analysis & Design

## 1. Context

PocketWorld is an existing personal/instanced-world plugin (`me.modify.pocketworld`, MC 1.19.2 / Java 17) that lets players create, theme, and share small private worlds. It currently works only because it hard-depends on the **SlimeWorldManager (SWM)** plugin for world storage. SWM's plugin-era architecture is dead — its successor, **AdvancedSlimePaper (ASP)**, abandoned the plugin model entirely and became a full Paper **server fork**, and the ASP maintainers' own code contains warnings telling anyone still running the old SWM plugin to remove it.

Goal: rebuild PocketWorld as a fully standalone plugin that implements the Hypixel **Slime Region Format (SRF)** itself — no ASP, no SWM, no server fork — while preserving PocketWorld's product intent (personal worlds, themes/templates, invitations, ranks) and fixing the real bugs found in the current implementation.

This required research before any design was possible, because the central question — *"can a plugin alone do what ASP's fork does?"* — has a hard, verifiable answer, not a matter of opinion.

## 2. Research Findings (condensed)

**ASP architecture** — `core`/`api`/`loaders` modules are **100% portable pure-Java** (verified via grep: zero `net.minecraft.*`/CraftBukkit imports). They do byte-level format read/write (`SlimeSerializer`, versioned readers v1_9→v13), an in-memory NBT-backed world model (`SkeletonSlimeWorld`), a from-scratch Anvil-format `.mca` reader/writer (`AnvilWorldReader`), and a storage-agnostic `SlimeLoader` interface (`byte[] readWorld/saveWorld`, file/MySQL/MongoDB/Redis/HTTP backends). None of that needs a fork.

What genuinely **requires** the fork: `aspaper-server`'s `SlimeLevelInstance extends net.minecraft.server.level.ServerLevel`, constructed directly with a custom `LevelStorageAccess` and wired into Paper's internal async chunk system (`ca.spottedleaf.moonrise.patches.chunk_system.*`) via custom `EntityDataController`/`PoiDataController` implementations, using an Access-Transformer file to widen NMS field visibility at build time. **A plugin cannot subclass `ServerLevel` or control which class the server instantiates for a world** — that specific mechanism is fork-only.

**The old plugin-era SWM** (pre-ASP) *did* run live, in-place chunk loading from a plain plugin — via per-Minecraft-version NMS submodules (`slimeworldmanager-nms-v1_8_R3` … `v1_15_R1`) implementing NMS's `IChunkLoader` directly and substituting it into a constructed `CustomWorldServer`. This broke down around 1.17–1.18 when Bukkit's chunk/ticket system changed enough that ASP's own README says the project "had to pivot to a Paper fork." Confirmed independently: **Paper's own maintainers rejected a request for a pluggable region-storage API** (GitHub Discussion #10559), explicitly citing unsustainable maintenance cost and pointing people at forks instead.

**Conclusion this design is built on:** there is no supported, stable way to make a live Bukkit `World` stream chunks from arbitrary storage without either (a) forking, or (b) reflection into Minecraft server internals that WILL break across versions and requires real, version-specific engineering. This project builds (b) for two named version floors (26.2 and 1.21.x) as a hands-on NMS project, with a reflection-free fallback ("Anvil-shadow", explained below) that always works, for every other version and as a safety net if an NMS adapter breaks.

**Versions**: Minecraft moved to `YY.D.H` versioning in late 2025. Current release is **Java Edition 26.2 "Chaos Cubed"** (released 2026-06-16); the prior scheme's final release was **1.21.11**. Current Paper (`io.papermc.paper:paper-api:26.2.build.+`) **requires Java 25** to run a 26.2 server.

**Libraries**: `com.github.luben:zstd-jni` (BSD-2-Clause) for compression — same library ASP uses, standard for this ecosystem. `net.kyori:adventure-nbt` (MIT) is a clean, well-tested NBT tree implementation ASP also uses — reusing it (as a dependency, not copying its code) avoids writing/maintaining our own NBT parser, and is shaded since it's not guaranteed present on every Paper build.

**Licensing**: ASP is GPLv3, no per-file headers, no dual license. Its *architecture and the SRF byte layout itself* (facts/format, not copyrightable) are fair to learn from; its *code* is not fair to copy or lightly adapt — doing so would create a GPLv3 obligation on our source. **This project is a clean-room re-implementation**: everything here is written from understanding, not from ASP's source.

**Existing PocketWorld repo** — solid, reusable UI/domain layer (menus, theme-creation wizard, invitations, ranks) sitting on top of SWM calls that must be fully replaced. Real bugs found that this rewrite fixes rather than reproduces:
- **Inverted invitation map on Mongo deserialize** (`MongoAdapter.pocketWorldFromDocument`) — invites silently stop resolving to their real recipient after any cache-miss reload.
- `ThemeLoader.deleteWorld()` calls `randomAccessFile.write(null)` → uncaught NPE.
- No mechanism unloads a world when its last player leaves (the code meant to do this is dead/orphaned, by its own comment "way too server intensive").
- `MySQLConnection` is an empty stub; selecting `mysql.use: true` alone passes config validation but NPEs on first DB call.
- `CommandTest` is an ungated debug command exposing raw inventory/item editing — must not ship.
- Main-thread-blocking synchronous Mongo calls inside `CreatureSpawnEvent` and several menu click handlers.
- No public API/events for other plugins to integrate with.
- No test suite at all.

## 3. Slime Region Format Primer

Binary layout (Hypixel SRF, as documented in AdvancedSlimePaper's `SLIME_FORMAT`, versions v1–v13): magic `0xB10B` + version byte, world DataVersion int, a flags bitmask (POI chunks / fluid ticks / block ticks present), then a zstd-compressed block of chunk data (per chunk: x/z, per-section light + block-state NBT + biome NBT, heightmaps, optional POI/tick data, tile entities, entities), followed by a separate zstd-compressed "extra" NBT compound reserved for custom/PDC data. Format evolved via those extension points (the "extra" tag, forward-compatible unknown-flag skipping) rather than breaking changes — we read v1–v13 for import/compatibility, and write using the same v13-compatible layout plus our own data inside the existing "extra" tag, so files we produce remain readable by other SRF tooling.

**Key insight for the runtime design**: because SRF chunk NBT is just "the same format as vanilla," and vanilla/Paper servers already carry Mojang's own DataFixerUpper to silently upgrade old-DataVersion world folders on load, our Anvil-shadow bridge (§8) gets cross-Minecraft-version chunk migration **for free** simply by writing technically-valid old-DataVersion Anvil NBT and letting the server's normal load path upgrade it — exactly like opening an old vanilla world. We do not need to reimplement Mojang's block-state/biome migrations ourselves.

## 4. High-Level Architecture

Two Maven modules under one reactor `pom.xml`, so the format/storage engine is independently testable and maintainable:

```
pocketworld/ (parent pom, packaging=pom, compiles to Java 21 bytecode)
├── pocketworld-slime/      -- format + storage engine (NO Bukkit/Paper dependency)
└── pocketworld-plugin/     -- Paper plugin: runtime, API, UI, commands (depends on pocketworld-slime)
```

Compile target is **Java 21 bytecode**, not 25: Paper 26.2 requires a Java 25 JVM to run, but our other supported floor (Paper 1.21.x) only requires Java 21. A single plugin jar must run on both, and newer JVMs can run older bytecode but not vice versa, so 21 is the target that works everywhere we support.

`pocketworld-slime` has zero Bukkit dependency so it can be unit-tested with plain JUnit and, if ever useful, reused outside this plugin. `pocketworld-plugin` is the only module that touches Bukkit/Paper/NMS.

## 5. Module: `pocketworld-slime`

```
com.pocketworld.slime
├── nbt/            -- thin wrapper around net.kyori:adventure-nbt (our own types where we need mutability helpers)
├── format/          -- binary container: header, flags, versioned reader/writer chain
│   ├── SlimeFormatVersion.java      (enum v1..v13, our own vNext reserved)
│   ├── SlimeReader.java / SlimeWriter.java   (top-level read(InputStream)/write(OutputStream, SlimeWorldData))
│   ├── reader/v10, v11, v12, v13/…  (per-version decode, mirroring the documented history)
│   └── migrate/UpgradeChain.java    (v1->v13 stepwise upgrade, run automatically on read)
├── model/           -- immutable in-memory world representation
│   ├── SlimeWorldData.java, SlimeChunkData.java, SlimeChunkSection.java
│   └── SlimeWorldProperties.java   (spawn, difficulty, biome, PVP, flags — our own property map, not ASP's)
├── anvil/           -- vanilla Anvil (.mca) reader/writer — clean-room, used by BOTH the importer AND the
│   │                   Anvil-shadow runtime bridge in pocketworld-plugin
│   ├── RegionFile.java (sector table, 4KiB sectors, zlib/gzip per chunk)
│   └── AnvilWorldReader.java / AnvilWorldWriter.java
├── compression/     -- Zstd(Input|Output)Stream wrappers over zstd-jni
├── storage/         -- SlimeLoader-equivalent, storage-agnostic
│   ├── WorldLoader.java   (interface: exists/read/write/delete/list, byte[]-level, storage-agnostic)
│   └── loader/{file,mysql,mongo}/…   (FileWorldLoader default; MysqlWorldLoader via HikariCP;
│                                        MongoWorldLoader via mongodb-driver-sync, GridFS for >16MB blobs)
├── validation/      -- checksum + structural validation, corruption detection/repair reporting
└── clone/           -- WorldCloner.java (duplicate a stored world under a new id/loader, streaming — no
                          full in-memory materialization needed for same-loader clones)
```

Key interface (storage-agnostic, deliberately small):
```java
public interface WorldLoader {
    boolean exists(String worldId) throws IOException;
    byte[] read(String worldId) throws IOException, UnknownWorldException;
    void write(String worldId, byte[] data) throws IOException;
    void delete(String worldId) throws IOException, UnknownWorldException;
    List<String> list() throws IOException;
}
```
This is intentionally close to what ASP's `SlimeLoader` looks like from the *outside* (it's the obvious shape for "byte blob store"), but written independently — no shared code.

## 6. Module: `pocketworld-plugin`

```
com.pocketworld.plugin
├── PocketWorldPlugin.java         -- bootstrap
├── api/            -- PUBLIC API for other plugins (does not exist today — new)
│   ├── PocketWorldAPI.java         (service, registered via ServicesManager)
│   └── event/  PocketWorldCreateEvent, PocketWorldLoadEvent, PocketWorldUnloadEvent,
│               PlayerEnterPocketWorldEvent, PlayerLeavePocketWorldEvent, ...
├── runtime/        -- the live world-lifecycle orchestration (NEW — this replaces every SlimePlugin call)
│   ├── PocketWorldRuntime.java     (create/load/unload/clone/import/export — orchestrates bridge + storage)
│   ├── bridge/
│   │   ├── WorldRuntimeBridge.java     (SPI: materialize(SlimeWorldData) -> live World; extract(World) -> SlimeWorldData)
│   │   ├── anvil/AnvilShadowBridge.java   (DEFAULT & fallback: writes a real temp Anvil folder via
│   │   │                                    pocketworld-slime's AnvilWorldWriter, Bukkit.createWorld(),
│   │   │                                    later AnvilWorldReader + Bukkit.unloadWorld() to re-export)
│   │   ├── nms/v1_21/Nms1_21Bridge.java   (reflection-based; spike-then-build, see Roadmap Stage 6)
│   │   ├── nms/v26_2/Nms26_2Bridge.java   (reflection-based; spike-then-build, see Roadmap Stage 7)
│   │   └── BridgeSelector.java     (fingerprints running server/version, picks best available bridge,
│   │                                 logs which mode is active, falls back to Anvil-shadow automatically)
├── world/          -- domain model (ported + cleaned from current repo)
│   ├── PocketWorld.java, WorldSpawn.java, WorldRank.java, Invitation.java (NEW explicit type, replaces
│   │                                                                        the buggy raw UUID map)
├── theme/          -- world templates (kept as "theme" naming from the existing product)
├── data/           -- metadata persistence: DAO/DataSource/Connection pattern KEPT from current repo,
│   │                  bug-fixed, MySQL actually implemented (HikariCP), Mongo modernized to
│   │                  mongodb-driver-sync 5.x
├── command/, listener/, ui/, util/   -- ported from current repo (menus, invitations UX, permissions),
│                                        CommandTest removed, plugin.yml permissions block added
└── compat/         -- version probing shared by BridgeSelector (Bukkit.getMinecraftVersion() etc.)
```

**Why keep the DAO/DataSource seam from the old repo**: it's already a clean boundary (`Connection.getDAO()`) that nothing else needs to change around — the whole `ui/` tree, `cache/`, and `command/` (minus `CommandTest`) don't touch SWM at all and port over close to unchanged.

## 7. World Lifecycle (both bridges, unified by `PocketWorldRuntime`)

**Create** (from theme): `WorldLoader.read(themeId)` → `SlimeReader` decodes bytes → `SlimeWorldData` in memory → `BridgeSelector.current().materialize(data, newWorldId)` → live Bukkit `World`. Metadata (`PocketWorld` domain object) created and persisted via `data/` layer, independent of which bridge ran.

**Load**: same as create, minus the clone step — `WorldLoader.read(worldId)` → decode → `materialize()`.

**Unload/Save**: `bridge.extract(world)` → `SlimeWorldData` → `SlimeWriter.write(...)` → `WorldLoader.write(worldId, bytes)` → `Bukkit.unloadWorld()` (Anvil-shadow also deletes its temp folder here; NMS bridges have no temp folder to clean).

**Clone**: pure storage-layer operation when source/target use the same loader (`WorldCloner` streams bytes, no bridge/decode needed); cross-loader clone decodes once and re-encodes to the target.

**Import/Export**: `AnvilWorldReader`/`AnvilWorldWriter` directly, no bridge involved — a first-class in-plugin command (`/pocketworldadmin import <folder>`), not a separate CLI jar.

## 8. Version Compatibility Strategy

- `BridgeSelector` runs once at startup: read `Bukkit.getMinecraftVersion()`/build info, check for a matching NMS bridge (`v1_21`, `v26_2`), and only select it after a **runtime fingerprint check** (reflectively confirm the expected internal fields/classes actually exist before trusting the bridge) — never assume a version string match means the internals still line up.
- If no NMS bridge matches, or the fingerprint check fails, fall back to `AnvilShadowBridge` automatically and log at WARN once (not spammy) so the server owner knows which mode they're in.
- All version-specific code lives under `runtime/bridge/nms/<version>` — nothing elsewhere in the plugin ever imports `net.minecraft.*` or references a Minecraft version.
- Minimum documented support: **Paper 26.2 (Java 25)** and **Paper 1.21.x (Java 21)** as the two version floors with dedicated NMS bridges; any other Paper version still runs correctly via Anvil-shadow, just without the NMS performance path. See `docs/COMPATIBILITY.md`.

## 9. Migration Strategy (existing PocketWorld code)

Not a line-by-line port — a rewrite of the SWM-facing 20% with the UI/domain 80% carried over deliberately:
- **Carry over near-unchanged**: `ui/` menu tree, `cache/`, most of `command/` (drop `CommandTest`), `util/`, `theme/` UI/state-machine (only its `slime.*` calls get repointed at `PocketWorldRuntime`).
- **Rewrite fully**: everything that called `SlimePlugin`/`SlimeLoader`/`SlimeWorld` — `PocketWorld.load/unload/delete`, `PocketWorldCreator`, `ThemeCreationController`'s world-generation calls, `SlimeHook`/`PocketHook` (delete — no external plugin dependency anymore), `MongoLoader`/`ThemeLoader` (replaced by `pocketworld-slime`'s storage backends).
- **Fix in place**: invitation model (introduce `Invitation{sender, recipient, timestamp}`, fixing the inversion bug), world-empty auto-unload (implement properly, configurable, replacing the dead orphaned method), MySQL support (real HikariCP-backed implementation), `plugin.yml` permissions block, remove the debug command.
- **New**: `api/` package (events + service) — nothing to port, since the current repo has no third-party API surface at all.

## 10. Testing Strategy

- **`pocketworld-slime`** (pure JUnit 5, no Bukkit): round-trip tests per format version (encode→decode→assert-equal) using small synthetic worlds; corruption tests (truncated/flipped-byte files must fail cleanly with a typed exception, not a crash); Anvil reader/writer round-trip against real small vanilla region files fixtures; loader backend tests (`FileWorldLoader` against a temp dir; `MysqlWorldLoader`/`MongoWorldLoader` against **Testcontainers** so CI doesn't need real external services).
- **`pocketworld-plugin`**: domain/unit tests for `PocketWorld`, `Invitation`, `WorldRank` with plain JUnit; integration-style tests using **MockBukkit** for command/listener/menu behavior where feasible; the NMS bridges are the one thing that can only really be verified on a real running Paper server of the matching version — called out explicitly rather than faked with a mock.

## 11. Risks & Technical Limitations

- **NMS bridges are the highest-risk, least-precedented part of this project.** No confirmed prior art exists for reflection-based live chunk-storage substitution against Paper's post-Moonrise chunk system (unlike the pre-1.17 era, where `IChunkLoader` substitution was common). Each NMS bridge stage starts with a small reflection **spike** to confirm a viable hook point exists before committing to a full implementation; if a version's internals don't allow a safe hook, that version simply runs on Anvil-shadow — the plugin never becomes non-functional.
- **Anvil-shadow performance**: reintroduces close to vanilla disk I/O/world-load latency for whichever versions don't have a working NMS bridge — a known, accepted trade-off (precedented by plugins like BedWars1058's map-reset system), not a bug.
- **zstd-jni is a native library** — must ship/shade correct natives for the platforms we support (documented alongside the storage layer).
- **Java 25 requirement** (for the 26.2 floor) drops compatibility with any host still on older JVMs; this is dictated by current Paper itself, not our choice.
- **GPLv3 exposure**: because this design was informed by studying ASP's source, all resulting `pocketworld-slime`/`pocketworld-plugin` code is written fresh (no copy-paste, no "translate variable names" adaptation) to avoid inheriting a GPLv3 obligation.
- **Data-version drift** between the Anvil-shadow bridge (gets Mojang's DFU for free via normal world-folder loading) and the NMS bridges (bypass that path) — NMS bridges only ever materialize slime data that's already at the current server's DataVersion; anything older is upgraded once via the Anvil-shadow path first (a format/migration-layer concern, not a per-load-event one).

## 12. Staged Implementation Roadmap

Each stage compiles, is tested, and is explained before moving on.

0. **Scaffold**: reactor `pom.xml`, both modules, empty-but-valid plugin jar. Docs (`README.md`, `docs/COMPATIBILITY.md`, this file).
1. **Format engine core**: `nbt`, `format` (readers v1–v13 + upgrade chain), `model`, `compression`. Unit tests: round-trip + corruption handling.
2. **Anvil layer**: `anvil/RegionFile`, `AnvilWorldReader`/`Writer`. Tests against small real vanilla region-file fixtures.
3. **Storage layer**: `WorldLoader` + File/MySQL/Mongo backends, `WorldCloner`. Tests (Testcontainers for MySQL/Mongo).
4. **Runtime skeleton + Anvil-shadow bridge**: `PocketWorldRuntime`, `WorldRuntimeBridge` SPI, `AnvilShadowBridge`, `BridgeSelector` (initially Anvil-shadow-only). This alone makes the plugin fully functional end-to-end (create/load/unload/clone/import/export) with zero NMS code — a real, shippable milestone.
5. **PocketWorld domain/API/UI port**: domain model, `data/` (bug-fixed DAO, real MySQL), theme system, UI/command/listener port, new public `api/` package + events, bug fixes (invitations, auto-unload, permissions, remove debug command). Manual in-game verification.
6. **NMS bridge spike — 1.21.x**: reflection-based hook-point investigation and, if viable, a working `Nms1_21Bridge`, benchmarked against Anvil-shadow.
7. **NMS bridge spike — 26.2**: same for the current version.
8. **Polish**: corruption/repair tooling, format migration command, compatibility docs finalized, permission defaults review.

## 13. Verification Approach

- Each of stages 1–3 verified with `mvn test` in `pocketworld-slime` alone (no server needed).
- Stage 4 onward verified by running a real Paper 26.2 server locally and exercising create/load/unload/clone/import through in-game commands.
- Stages 6–7 additionally verified against a real Paper 1.21.x server instance, comparing world-load timing/behavior against the Anvil-shadow path to confirm the NMS bridge is actually faster and correct before recommending it as default for that version.
