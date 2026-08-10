# PocketWorld

A standalone Paper plugin for personal/instanced player worlds. PocketWorld implements the
Hypixel **Slime Region Format (SRF)** itself — there is no dependency on AdvancedSlimePaper,
SlimeWorldManager, or any custom server fork. It is a normal, installable plugin jar.

Every player can create their own small, private "pocket" world from a theme (a template an admin
builds once), invite friends in with their own rank, tweak world settings, and hop between worlds
through a menu — all backed by a compact, portable storage format rather than sprawling folders of
loose region files.

## Features

- **Personal pocket worlds.** Any player can create up to a configurable number of their own worlds
  from an existing theme, give each one a name, and load/unload/delete them independently. A world
  automatically unloads a configurable delay after the last member leaves it (and the pending unload
  cancels itself if someone rejoins first), so idle worlds don't sit around consuming resources.
- **Themes.** Admins build a theme once, as a real void world with a bordered build area, using
  `/theme create`. Every pocket world made from that theme starts as an exact copy of it. Themes are
  managed (listed, deleted) independently of the worlds created from them.
- **Ranks and shared ownership.** Every member of a pocket world has a rank - **Owner** (full
  control, including editing other members' ranks and deleting the world), **Mod** (can invite and
  kick players by default), or **Member** (no elevated permissions by default). A world can have any
  number of members at any rank.
- **Visitor permissions.** Anyone physically inside a world who isn't a member is a **Visitor**. An
  owner can configure, per rank (Visitor/Member/Mod), exactly which actions are allowed - build,
  break, and interact for visitors; invite, kick, and set-spawn for members/mods - from a dedicated
  Permissions menu, and can expel every visitor from the world in one click. Defaults match the
  original hardcoded behavior, so nothing changes until an owner opens the menu.
- **Invitations.** An owner or mod (or anyone else granted the invite permission) can invite any
  online player by typing their username in chat; the invited player accepts or declines from their
  own Invitations menu, and a pending invitation can be revoked before it's answered.
- **Per-world settings.** Each world's owner can toggle PVP, animal spawns, and monster spawns
  independently from the World Properties menu, and whoever holds the set-spawn permission (the owner,
  by default) can set the world's spawn point to their current in-world position - no commands needed.
- **World teleportation menu.** Jump directly to any world you're a member of from a single menu,
  without needing to remember or type its name.
- **World creation queue.** World creation/loading is serialized server-wide (toggleable in
  `config.yml`) so a burst of simultaneous requests can't stack their main-thread cost into one long
  freeze - queued players see a live position indicator instead.
- **Corruption detection.** Every stored world's bytes can be checked for structural corruption
  on demand (`/pocketworldadmin validate`), and a corrupted world fails with a clear message to both
  the affected player and the server log instead of silently doing nothing.
- **Import/export tooling.** Move a real, ordinary Anvil-format world folder in or out of PocketWorld's
  storage with a single admin command - useful for backups, recovery, or bootstrapping a theme from
  an existing build.
- **Public API for other plugins.** A `PocketWorldAPI` service (fetched via Bukkit's
  `ServicesManager`) plus five events - `PocketWorldCreateEvent`, `PocketWorldLoadEvent`,
  `PocketWorldUnloadEvent`, `PlayerEnterPocketWorldEvent`, `PlayerLeavePocketWorldEvent` - let other
  plugins react to pocket world activity without touching PocketWorld's internals.
- **Choice of metadata storage.** World/theme/user records (not the world data itself, see below) can
  live in local YAML files (the zero-setup default), MySQL, or MongoDB - pick one in `config.yml`.
- **No server fork, no external dependency.** The Slime Region Format - normally something you'd need
  AdvancedSlimePaper's forked server for - is implemented directly in this plugin
  (`pocketworld-slime`, a standalone module with no Bukkit dependency of its own). Install it like any
  other plugin on stock Paper.

## Minimum supported versions

See [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) for the full, current answer. Summary:

| | Minimum |
|---|---|
| Minecraft / Paper | 1.21.x, and current 26.2 |
| Java | 21 (to build and to run on the 1.21.x floor); 25 required to run a 26.2 server |
| Build tool | Maven 3.9+ |

## Commands

| Command | Description |
|---|---|
| `/pocketworld` (`/pw`) | Opens the main PocketWorld menu - create, manage, teleport to, and receive invitations for your own pocket worlds. Takes no arguments; everything past this is menu-driven. |
| `/theme <create\|manage\|delete\|import\|edit>` | Build and manage the themes players create worlds from. `import` and `edit` are recognized but not yet implemented. |
| `/pocketworldadmin` (`/pwa`) `<reload\|import\|export\|validate\|manage>` | Server administration: reload config files, move a stored pocket world in/out of a real Anvil folder, check stored world data for corruption, or browse/manage any player's pocket worlds. Running it with no arguments prints usage help. |

### `/theme` sub-arguments

| Sub-argument | Usage | Description |
|---|---|---|
| `create` | `/theme create` | Starts the theme-creation wizard: name, biome, icon, description, then drops you into a fresh void editor world to build in. |
| `manage` (alias `list`) | `/theme manage` | Lists every theme, with clickable buttons to edit or delete each one. |
| `delete` | `/theme delete <id>` | Permanently deletes a theme by its UUID (shown in the manage list). |
| `import` | `/theme import` | Not yet implemented. |
| `edit` | `/theme edit` | Not yet implemented. |

### `/pocketworldadmin` sub-arguments

| Sub-argument | Usage | Description |
|---|---|---|
| `reload` | `/pocketworldadmin reload` | Reloads `config.yml` and `messages.yml` without restarting the server. |
| `import` | `/pocketworldadmin import <folder> <worldId> [dataVersion]` | Imports a real Anvil-format world folder (a `region/` + `entities/` pair) as a stored pocket world under `<worldId>`. `folder` is resolved relative to the server's root directory unless given as an absolute path. `dataVersion` defaults to the running server's own version if omitted - only pass one explicitly for a folder from an older Minecraft version. Refuses to overwrite an existing world id. |
| `export` | `/pocketworldadmin export <worldId> <folder>` | Exports a stored pocket world back out as a real Anvil-format world folder at `folder`. Refuses to write into a path that already exists. |
| `validate` | `/pocketworldadmin validate <worldId\|all>` | Decodes a stored world's bytes (or every stored world, with `all`) and reports whether each one is structurally valid or corrupted, without needing to actually load it in-game first. |
| `manage` | `/pocketworldadmin manage [player]` | With no name, opens a paginated grid of every online player's skull; with a name, resolves that player directly (online or offline). Either way opens a menu with **Worlds** (every pocket world the player is a member of - view members, resize the world border, wipe it, or teleport yourself/another player into it) and **Punish** (placeholder, no punishments configured yet). |

## Permissions

Every command that takes sub-arguments has its own base "can you run this command at all" node, plus
one further node per sub-argument - so, for example, an admin can grant `pocketworld.theme.manage`
without also handing out `pocketworld.theme.delete`. `/pocketworld` has no sub-arguments, so it has
just the one node.

| Node | Default | Grants |
|---|---|---|
| `pocketworld.*` | op | Every node below, in one grant. |
| `pocketworld.command.pocketworld` | **true** (everyone) | Use `/pocketworld` at all. |
| `pocketworld.command.theme` | op | Use `/theme` at all. Also requires the matching node below for whichever sub-argument is used. |
| `pocketworld.theme.create` | op | `/theme create` |
| `pocketworld.theme.manage` | op | `/theme manage` (and its `list` alias) |
| `pocketworld.theme.delete` | op | `/theme delete` |
| `pocketworld.theme.import` | op | `/theme import` (not yet implemented) |
| `pocketworld.theme.edit` | op | `/theme edit` (not yet implemented) |
| `pocketworld.command.admin` | op | Use `/pocketworldadmin` at all. Also requires the matching node below for whichever sub-argument is used. |
| `pocketworld.admin.reload` | op | `/pocketworldadmin reload` |
| `pocketworld.admin.import` | op | `/pocketworldadmin import` |
| `pocketworld.admin.export` | op | `/pocketworldadmin export` |
| `pocketworld.admin.validate` | op | `/pocketworldadmin validate` |
| `pocketworld.admin.manage` | op | `/pocketworldadmin manage` - browsing and managing any player's pocket worlds |

## Configuration

`config.yml`, generated on first run:

| Key | Default | Description |
|---|---|---|
| `debug` | `false` | Verbose diagnostic logging (world load/unload timings, which runtime bridge was selected, etc). |
| `general.max-worlds` | `5` | Maximum pocket worlds a single player may own at once. |
| `general.auto-unload-delay-seconds` | `60` | How long an empty, loaded pocket world waits before auto-unloading. Cancelled if a member rejoins first. |
| `general.creation-queue-enabled` | `true` | Serializes world creation/loading server-wide so a burst of simultaneous requests can't stack into one long main-thread freeze. Disable to let every request start immediately instead. |
| `world-difficulty` | `normal` | Difficulty applied to every pocket world. One of `peaceful`, `easy`, `normal`, `hard`. |
| `mongodb.use` / `mysql.use` | `false` / `false` | Which backend stores world/theme/user *metadata* (not the world data itself - see Features above). Local YAML files are used if neither is enabled; enabling both at once is an error. |

## Project layout

- **`pocketworld-slime`** — the Slime format/storage engine: binary read/write, compression,
  in-memory world model, Anvil (`.mca`) import/export, and pluggable storage backends
  (file/MySQL/MongoDB). Has no dependency on Bukkit/Paper — built and tested independently.
- **`pocketworld-plugin`** — the Paper plugin: world lifecycle/runtime, the PocketWorld domain
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
extended live play on real Paper 1.21.x and 26.2 servers, including several bugs found and fixed
from that testing rather than assumed away.
