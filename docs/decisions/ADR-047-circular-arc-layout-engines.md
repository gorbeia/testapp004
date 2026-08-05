# ADR-047 — Circular and Arc Layout Engines + Ego-Centric Radial Fix

**Date:** 2026-08-05
**Status:** Accepted

---

## Context

The `:canvas` library exposed three layout engines (Hierarchical, Force-Directed, Radial).
User review revealed two gaps:

1. The existing `RadialLayoutEngine` always arranged nodes as disconnected clusters,
   ignoring the `rootId` parameter. The user expected an ego-centric layout where the
   root sits at the origin and all other nodes are placed on concentric rings by BFS
   distance from the root.

2. Two additional layout styles were requested: **Circular** (nodes equidistantly
   spaced on a circle) and **Arc** (nodes aligned on a horizontal line, edges best
   viewed as semi-circular arcs).

---

## Decision

### 1. `RadialLayoutEngine` — ego-centric mode when `rootId` is provided

`computePositions()` now dispatches to a private `computeEgoCentric()` when `rootId`
is non-null:

- Root placed at `(0, 0)`.
- BFS from root assigns a distance to every reachable node; unreachable nodes get
  `maxDistance + 1`.
- Nodes at distance `d` are placed uniformly on a circle of radius `d × 200 px`.
- Disconnected-component fallback is preserved for the no-root case.

Shared private helpers `buildAdjacency()` and `bfsDistances()` were extracted at
file scope so the new engines can reuse them.

### 2. `CircularLayoutEngine` — new engine

All nodes are placed equidistantly around a single circle. Radius is derived from
the node count (`n × 240 / 2π`, min 180 px). When a `rootId` is provided the root
appears at 12 o'clock and remaining nodes follow in BFS order; without a root, nodes
are sorted by id.

### 3. `ArcLayoutEngine` — new engine

All nodes are placed on a horizontal line (`y = 0`). When a `rootId` is provided,
the root is the pivot (horizontal centre) and BFS neighbours are interleaved left and
right; without a root, nodes are sorted by degree descending. This layout is designed
to be paired with `forceArcs = true` on the `RelationGraph` composable so every edge
renders as a semi-circular arc regardless of parallelism.

### 4. `forceArcs: Boolean = false` parameter on `RelationGraph`

A single-edge pair would normally render as a straight line. `forceArcs = true` forces
the `arcIndex` to `1` for every singleton edge group, making all edges arc above the
horizontal baseline. Call sites pass `forceArcs = uiState.layoutEngineType == LayoutEngineType.Arc`.

### 5. `LayoutEngineType` extended with `Circular` and `Arc`

Both enum values are wired into `createEngine()`. The control sheet in both canvas
screens uses `FlowRow` instead of `Row` so that five chips wrap gracefully on narrow
displays.

### 6. `CategoryCanvasViewModel` — rootId for Radial

The existing `when` block that computed `rootId` only for `Hierarchical` is extended
to also cover `Radial`. The most-connected person in the visible set becomes the ego
node. `PersonCanvasViewModel` already forwards `acquaintanceId` as `rootId` for all
engine types — no change needed there.

---

## Consequences

- Both canvas screens now offer five layout engines: Hierarchical, Force-Directed,
  Radial (ego-centric), Circular, Arc.
- The Arc engine is only visually compelling when `forceArcs` is active; the
  composable handles this automatically based on `uiState.layoutEngineType`.
- `FlowRow` (Compose `ExperimentalLayoutApi`) is used in both control sheets for
  chip wrapping; the `@OptIn` annotation is applied locally to the composable.
- Existing behaviour of the other three engines and all call sites outside the
  two canvas screens is unchanged.
