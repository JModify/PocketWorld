# Compatibility

## Minimum supported versions

- **Minecraft / Paper: 1.21.x** and **26.2** ("Chaos Cubed") are the two version floors with a
  dedicated, tested runtime bridge (see `docs/ARCHITECTURE.md` §8). Any other Paper version the
  server is running on will still load correctly through the Anvil-shadow fallback bridge — it is
  never a hard failure, just a slower code path with no version-specific code behind it.
- **Java**:
  - 21 minimum to build the project and to run a Paper 1.21.x server.
  - 25 required to run a Paper 26.2 server (Paper's own requirement, not ours).
  - The plugin jar itself is compiled to Java 21 bytecode class files so a single jar runs
    unmodified on both a Java 21 and a Java 25 server JVM.
- **Maven**: 3.9+ (developed against 3.9.9).

## Why two explicit version floors

Paper has no supported API for a plugin to control how chunks/worlds are read from or written to
storage (confirmed via Paper's own maintainers rejecting this as a feature request — see
`docs/ARCHITECTURE.md` §2). Truly live, Slime-native chunk loading therefore requires
version-specific reflection into server internals. Rather than either avoiding that entirely or
requiring a server fork, this project builds real per-version bridges for the two floors above,
isolated behind a single internal interface (`WorldRuntimeBridge`), and falls back automatically
to a reflection-free, public-API-only bridge for every other version.

## Checking which mode is active

At startup the plugin logs which runtime bridge it selected for the currently running server
build, once, at `WARN` level if it fell back to the Anvil-shadow bridge. (Exact log line will be
finalized alongside the `BridgeSelector` implementation in the runtime stage.)
