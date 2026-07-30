# PocketWorld

A standalone Paper plugin for personal/instanced player worlds. PocketWorld implements the
Hypixel **Slime Region Format (SRF)** itself — there is no dependency on AdvancedSlimePaper,
SlimeWorldManager, or any custom server fork. It is a normal, installable plugin jar.

## Minimum supported versions

See [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) for the full, current answer. Summary:

| | Minimum |
|---|---|
| Minecraft / Paper | 1.21.x, and current 26.2 |
| Java | 21 (to build and to run on the 1.21.x floor); 25 required to run a 26.2 server |
| Build tool | Maven 3.9+ |

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

The final plugin jar is produced at `pocketworld-plugin/target/pocketworld-plugin-<version>.jar`.

## Status

Early development, built in verified stages — see `docs/ARCHITECTURE.md` §12 for the roadmap.
