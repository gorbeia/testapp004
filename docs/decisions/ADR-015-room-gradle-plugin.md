# ADR-015: Room Gradle Plugin for variant-safe schema export

**Date:** 2026-06-01
**Status:** Accepted

## Context

With `exportSchema = true` and a shared `ksp { arg("room.schemaLocation", ...) }`, both
`kspDebugKotlin` and `kspReleaseKotlin` Gradle tasks write to the same schema file path
concurrently. One task truncates the file while the other reads it, causing Room KSP to
throw `IllegalStateException: Empty schema file` and failing CI.

## Decision

Apply the official `androidx.room` Gradle plugin (version-matched to the Room library,
currently 2.6.1) and replace the bare KSP argument with the plugin's `room { schemaDirectory() }`
DSL. The plugin routes schema writes through proper Gradle task outputs with variant-aware
paths, eliminating the race condition.

## Alternatives Considered

| Option | Why rejected |
|--------|-------------|
| Point `room.schemaLocation` at `layout.buildDirectory` | Schemas would not be checked into VCS; loses migration history |
| Set per-variant KSP args via `androidComponents` | Verbose, fragile, not the supported API for Room schema config |
| Disable Gradle parallel execution for KSP tasks | Global performance regression; treats the symptom, not the cause |

## Consequences

- CI parallel builds of debug and release variants no longer race on the schema file.
- Schema files are now written to variant-specific subdirectories under `app/schemas/`.
- No change to Room library version or any runtime behaviour.
