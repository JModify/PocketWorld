# Compatibility

## Minimum supported versions

- **Minecraft / Paper: 1.21.x** and **26.2** ("Chaos Cubed") are the two documented version floors,
  both empirically verified end-to-end (see `docs/ARCHITECTURE.md` §13/§16). Any other Paper version
  in between is expected to behave the same way, though only these two have actually been tested.
- **Java**:
  - 21 minimum to build the project and to run a Paper 1.21.x server.
  - 25 required to run a Paper 26.2 server (Paper's own requirement, not ours).
  - The plugin jar itself is compiled to Java 21 bytecode class files so a single jar runs
    unmodified on both a Java 21 and a Java 25 server JVM.
- **Maven**: 3.9+ (developed against 3.9.9).

## One runtime bridge, not one per version

Earlier planning called for a dedicated, reflection-based runtime bridge per version floor. That
was investigated properly, against real patched server jars for both version floors separately
rather than assuming one result carries over to the other (see `docs/ARCHITECTURE.md` §2, §11, §15,
§17), and concluded not to be safely
buildable without forking the server — a live custom-chunk-storage bridge needs to be present
*during* `ServerLevel`'s own constructor, which only subclassing (a fork) allows; reflecting into an
already-constructed world is too late, since everything downstream has already wired itself to the
original objects. Paper's own maintainers have separately confirmed there's no supported API for
this either (they rejected a pluggable region-storage API as a feature request, citing maintenance
cost).

So there is exactly one runtime bridge (`AnvilShadowBridge`), and every supported Paper version runs
it. It materializes stored world data as a real, temporary vanilla-format world folder and lets
Paper's own world loader do the rest — no NMS, no reflection, no version-specific code. The seam for
a future bridge (`WorldRuntimeBridge`, picked via `BridgeSelector`) is still in place in case a
genuinely viable one turns up, but nothing today depends on that happening.

## The two floors are not equally fast, and that's expected

The bridge keeps a world's on-disk folder as a warm, in-session cache after a normal unload instead
of rebuilding it from scratch on every load — a world revisited soon after last use skips the
decode-and-rewrite step entirely. Whether that actually happens depends on the platform:

- **Paper 1.21.x**: the cache reliably works. A revisited world's second load was measured at 0ms
  (down from ~418ms cold), with its on-disk files provably untouched.
- **Paper 26.2**: the cache never hits, because Paper 26.2's own world-creation migration
  (`LegacyCraftBukkitWorldMigration`) relocates a newly created world's files to a different final
  location on every single activation. The bridge's freshness check only trusts the classic
  pre-migration location, so it safely (and correctly) detects "this isn't where I left it" and
  falls back to a full rebuild every time, rather than risking stale data. This is a real, measured
  performance difference between the two floors, not a bug — see `docs/ARCHITECTURE.md` §16 for the
  full investigation, including a real bug an earlier, less careful version of this design had here.

## Checking which mode is active

At startup the plugin logs which runtime bridge it selected, once, at `INFO`:

```
[PocketWorld] PocketWorld: no version-specific runtime bridge for this server; using the
Anvil-shadow fallback (slower world load/unload, fully functional).
```

Today this is the only message you'll ever see, since Anvil-shadow is the only bridge that exists.
