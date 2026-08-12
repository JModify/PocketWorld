# PocketWorld

A plugin for personal, instanced player worlds, built primarily for Paper - it also runs on plain
Spigot (same 1.21.x and current 26.2 version floors), though world creation is noticeably slower
there; see "Paper vs. Spigot" below. Every player can create their own small, private "pocket"
world from a theme (a template an admin builds once), invite friends in with their own rank, tweak
world settings, and hop between worlds through a menu.

Worlds are stored using Hypixel's **Slime Region Format (SRF)**, implemented directly in the
plugin rather than requiring a forked server - so it installs like any other plugin jar, at the
cost of the world-loading speed a true fork gets from keeping everything in memory. What the
format still gets you: a compact, portable footprint per world instead of sprawling folders of
loose region files.

## Features

- **Personal pocket worlds.** Any player can create up to a configurable number of their own worlds
  from an existing theme, give each one a name, and load/unload/delete them independently. A world
  automatically unloads a configurable delay after the last member leaves it (and the pending unload
  cancels itself if someone rejoins first), so idle worlds don't sit around consuming resources.
- **Themes.** Admins build a theme once, as a real void world with a bordered build area, using
  `/theme create`. Every pocket world made from that theme starts as an exact copy of it. Themes are
  managed (viewed, deleted) from `/theme manage`'s own menu, independently of the worlds created
  from them.
- **Ranks and shared ownership.** Every member of a pocket world has a rank - **Owner** (full
  control, including editing other members' ranks and deleting the world), **Mod** (can invite and
  kick players by default), or **Member** (no elevated permissions by default). A world can have any
  number of members at any rank.
- **Visitor permissions.** Anyone physically inside a world who isn't a member is a **Visitor**. An
  owner can configure, per rank (Visitor/Member/Mod), exactly which actions are allowed - build,
  break, and interact for visitors; invite, kick, and set-spawn for members/mods - from a dedicated
  Permissions menu, and can expel every visitor from the world in one click. Every world starts
  with sensible defaults, so nothing changes until an owner opens the menu.
- **Invitations.** An owner or mod (or anyone else granted the invite permission) can invite any
  online player by typing their username in chat; the invited player accepts or declines from their
  own Invitations menu, and a pending invitation can be revoked before it's answered.
- **Per-world settings.** Each world's owner can toggle PVP, animal spawns, and monster spawns
  independently from the World Properties menu, and whoever holds the set-spawn permission (the owner,
  by default) can set the world's spawn point to their current in-world position - no commands needed.
- **World teleportation menu.** Jump directly to any world you're a member of from a single menu,
  without needing to remember or type its name.
- **World creation queue.** World creation/loading is serialized server-wide (toggleable in
  `config.yml`), so a burst of players creating worlds at the same moment queues up one at a time
  instead of piling into one longer stall - queued players see a live position indicator instead.
  See "Paper vs. Spigot" below for what each individual creation costs.
- **Corruption detection.** Any stored world can be checked for corruption on demand
  (`/pocketworldadmin validate`), and a corrupted world fails with a clear message to both the
  affected player and the server log instead of silently doing nothing.
- **Import/export tooling.** Move a standard Anvil-format world folder in or out of PocketWorld's
  storage with a single admin command - useful for backups, recovery, or bootstrapping a theme from
  an existing build.
- **Public API for other plugins.** A `PocketWorldAPI` service (fetched via Bukkit's
  `ServicesManager`) plus five events - `PocketWorldCreateEvent`, `PocketWorldLoadEvent`,
  `PocketWorldUnloadEvent`, `PlayerEnterPocketWorldEvent`, `PlayerLeavePocketWorldEvent` - let other
  plugins react to pocket world activity without touching PocketWorld's internals.
- **Choice of metadata storage.** World/theme/user records (not the world data itself, see below) can
  live in local YAML files (the zero-setup default), MySQL, or MongoDB - pick one in `config.yml`.

## Minimum supported versions

See [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) for the full, current answer. Summary:

| | Minimum |
|---|---|
| Minecraft | 1.21.x, and current 26.2 |
| Server software | Paper or Spigot |
| Java | 21 (to build and to run on the 1.21.x floor); 25 required to run a 26.2 server |
| Build tool | Maven 3.9+ |

## Paper vs. Spigot

**Paper is highly recommended.** Spigot support is real and fully functional, but we only
recommend it for small servers - roughly under 10-20 concurrent players. The reason is structural,
not a bug: Spigot has no async chunk-loading API, so bringing a newly-created pocket world online
has to finish that work synchronously on the main thread instead of in the background. On a busy
server that shows up as a brief (roughly 1-1.5 second) freeze felt by *every* player online, not
just the one creating a world, and it can stack up if several players create worlds around the same
time. On Paper the same work happens off-thread, so only the creating player notices a short
loading pause and nobody else is affected. If you're running a larger, more populated server, use
Paper.

## Commands

| Command | Description |
|---|---|
| `/pocketworld` (`/pw`) | Opens the main PocketWorld menu - create, manage, teleport to, and receive invitations for your own pocket worlds. Takes no arguments; everything past this is menu-driven. |
| `/theme <create\|manage\|import\|edit>` | Build and manage the themes players create worlds from. `import` and `edit` are recognized but not yet implemented. |
| `/pocketworldadmin` (`/pwa`) `<reload\|import\|export\|validate\|manage\|bypass>` | Server administration: reload config files, move a stored pocket world in/out of a real Anvil folder, check stored world data for corruption, browse/manage any player's pocket worlds, or toggle bypassing visitor permissions. Running it with no arguments prints usage help. |

### `/theme` sub-arguments

| Sub-argument | Usage | Description |
|---|---|---|
| `create` | `/theme create` | Starts the theme-creation wizard: name, biome, icon, description, then drops you into a fresh void editor world to build in. |
| `manage` (alias `list`) | `/theme manage` | Opens a paginated menu of every theme; clicking one opens its own menu to view details, delete it (with confirmation), or edit it (not yet implemented). |
| `import` | `/theme import` | Not yet implemented. |
| `edit` | `/theme edit` | Not yet implemented. |

### `/pocketworldadmin` sub-arguments

| Sub-argument | Usage | Description |
|---|---|---|
| `reload` | `/pocketworldadmin reload` | Reloads `config.yml` and `messages.yml` without restarting the server. |
| `import` | `/pocketworldadmin import <folder> <worldId> [dataVersion]` | Imports a real Anvil-format world folder (a `region/` + `entities/` pair) as a stored pocket world under `<worldId>`. `folder` is resolved relative to the server's root directory unless given as an absolute path. `dataVersion` defaults to the running server's own version if omitted - only pass one explicitly for a folder from an older Minecraft version. Refuses to overwrite an existing world id. |
| `export` | `/pocketworldadmin export <worldId> <folder>` | Exports a stored pocket world back out as a real Anvil-format world folder at `folder`. Refuses to write into a path that already exists. |
| `validate` | `/pocketworldadmin validate <worldId\|all>` | Decodes a stored world's bytes (or every stored world, with `all`) and reports whether each one is structurally valid or corrupted, without needing to actually load it in-game first. |
| `manage` | `/pocketworldadmin manage [player]` | With no name, opens a paginated grid of every online player's skull; with a name, resolves that player directly (online or offline). Either way opens a menu with **Worlds** (every pocket world the player is a member of - view/manage members as if you owned the world, resize the world border, wipe it, or teleport yourself/another player into it) and **Punish** (placeholder, no punishments configured yet). |
| `bypass` | `/pocketworldadmin bypass` | Toggles bypassing visitor build/break/interact permission enforcement in every pocket world for yourself, for the rest of your session (not persisted across a restart). |

## Permissions

Every command that takes sub-arguments has its own base "can you run this command at all" node, plus
one further node per sub-argument. For example, an admin can grant `pocketworld.theme.manage`
without also handing out `pocketworld.theme.create`. `/pocketworld` has no sub-arguments, so it has
just the one node.

| Node | Default | Grants |
|---|---|---|
| `pocketworld.*` | op | Every node below, in one grant. |
| `pocketworld.command.pocketworld` | **true** (everyone) | Use `/pocketworld` at all. |
| `pocketworld.command.theme` | op | Use `/theme` at all. Also requires the matching node below for whichever sub-argument is used. |
| `pocketworld.theme.create` | op | `/theme create` |
| `pocketworld.theme.manage` | op | `/theme manage` (and its `list` alias) - also covers deleting a theme from that menu |
| `pocketworld.theme.import` | op | `/theme import` (not yet implemented) |
| `pocketworld.theme.edit` | op | `/theme edit` (not yet implemented) |
| `pocketworld.command.admin` | op | Use `/pocketworldadmin` at all. Also requires the matching node below for whichever sub-argument is used. |
| `pocketworld.admin.reload` | op | `/pocketworldadmin reload` |
| `pocketworld.admin.import` | op | `/pocketworldadmin import` |
| `pocketworld.admin.export` | op | `/pocketworldadmin export` |
| `pocketworld.admin.validate` | op | `/pocketworldadmin validate` |
| `pocketworld.admin.manage` | op | `/pocketworldadmin manage` - browsing and managing any player's pocket worlds |
| `pocketworld.admin.bypass` | op | `/pocketworldadmin bypass` - toggles bypassing visitor build/break/interact permissions in any pocket world |

## Configuration

`config.yml`, generated on first run:

| Key | Default | Description |
|---|---|---|
| `debug` | `false` | Verbose diagnostic logging (world load/unload timings, which runtime bridge was selected, etc). |
| `general.max-worlds` | `5` | Maximum pocket worlds a single player may own at once. |
| `general.auto-unload-delay-seconds` | `60` | How long an empty, loaded pocket world waits before auto-unloading. Cancelled if a member rejoins first. |
| `general.creation-queue-enabled` | `true` | Serializes world creation/loading server-wide so a burst of simultaneous requests can't pile up into one long freeze. Disable to let every request start immediately instead. |
| `general.creation-queue-delay-seconds` | `0` | Extra pause between one queued creation/load finishing and the next one starting, spreading server load out further than the queue alone. Has no effect when the queue is disabled. |
| `world-difficulty` | `normal` | Difficulty applied to every pocket world. One of `peaceful`, `easy`, `normal`, `hard`. |
| `mongodb.use` / `mysql.use` | `false` / `false` | Which backend stores world/theme/user *metadata* (not the world data itself - see Features above). Local YAML files are used if neither is enabled; enabling both at once is an error. |

## Project layout

- **`pocketworld-slime`** - the Slime format/storage engine: binary read/write, compression,
  in-memory world model, Anvil (`.mca`) import/export, and pluggable storage backends
  (file/MySQL/MongoDB). Has no dependency on Bukkit/Paper - built and tested independently.
- **`pocketworld-plugin`** - the Paper plugin: world lifecycle/runtime, the PocketWorld domain
  model (worlds, themes/templates, ranks, invitations), commands, menus, configuration,
  permissions, and a public API for other plugins.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the full technical design and rationale.

## Building

```
mvn -pl pocketworld-slime,pocketworld-plugin -am package
```

The final, shaded plugin jar is produced at `pocketworld-plugin/target/PocketWorld-<version>.jar`.

## Status

Feature-complete against the original design (`docs/ARCHITECTURE.md` §12) and verified through
extended live play on real Paper 1.21.x and 26.2 servers, with several bugs caught and fixed
along the way. Spigot support has also had real live testing, on an actual Spigot 26.2 server,
which caught and fixed two platform-specific bugs (a missing world-gen-settings file, and pocket
worlds loading in as empty voids - see `docs/ARCHITECTURE.md` §21/§22). See "Paper vs. Spigot"
above for the performance guidance that testing led to.
