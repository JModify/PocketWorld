# Compatibility

## Minimum supported versions

- **Minecraft: 1.21.x** and **26.2** ("Chaos Cubed") are the two documented version floors, on
  either **Paper** or **Spigot**. Paper is the primary target, empirically verified end-to-end (see
  `docs/ARCHITECTURE.md` §13/§16); see "Spigot support" below for exactly what running on Spigot
  does and doesn't have behind it. Any other Paper/Spigot version in between the two floors is
  expected to behave the same way, though only these two have actually been tested.
- **Java**:
  - 21 minimum to build the project and to run a 1.21.x server.
  - 25 required to run a 26.2 server (Paper/Spigot's own requirement, not ours).
  - The plugin jar itself is compiled to Java 21 bytecode class files so a single jar runs
    unmodified on both a Java 21 and a Java 25 server JVM.
- **Maven**: 3.9+ (developed against 3.9.9).

## Spigot support

The plugin compiles against `paper-api`, deliberately - it's a strict superset of `spigot-api`
(Paper only ever adds to it, never removes), so a jar that never calls a Paper-exclusive symbol
runs correctly on both platforms from that same compile target. A full-tree audit found every
Paper-only call site in use and replaced each with the plain Bukkit/Spigot equivalent that also
works unmodified on Paper - see `docs/ARCHITECTURE.md` §20 for the complete list and the reasoning
behind each swap.

The one real behavioral difference is `ChunkPrewarmer` (`util/ChunkPrewarmer.java`): during world
creation it uses Paper's `getChunkAtAsync` to move a small chunk-loading cost off the main thread
when that method is actually present, detected safely via reflection so it can never throw
`NoSuchMethodError` on Spigot. On Spigot it falls back to a plain synchronous touch instead -
slower by a few hundred milliseconds, not the multiple seconds this used to mean before an
unrelated fix; see `docs/ARCHITECTURE.md` §20 for the real measurements on both paths.

What's actually been verified: the API-compatibility swap, by re-grepping the whole tree for
Paper-only imports after the fact; and `ChunkPrewarmer`'s reflection path, live against a real
Paper server (it correctly detects the method and correctly falls back to the main thread
afterward). What hasn't been verified is a full Spigot server actually running the plugin
end-to-end - no Spigot jar was available in that testing environment (Spigot doesn't publish one to
a public repo; building it requires running BuildTools.jar locally). The Spigot fallback path
itself is simple enough (one synchronous `getChunkAt` call) to be low-risk by inspection, but
that's a weaker claim than this project's usual standard of verifying empirically, worth stating
plainly rather than glossing over.

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

So there is exactly one runtime bridge (`AnvilShadowBridge`), and every supported Paper or Spigot
version runs it. It materializes stored world data as a real, temporary vanilla-format world folder
and lets the platform's own world loader do the rest — no NMS, no reflection, no version-specific
code. The seam for a future bridge (`WorldRuntimeBridge`, picked via `BridgeSelector`) is still in
place in case a genuinely viable one turns up, but nothing today depends on that happening.

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
