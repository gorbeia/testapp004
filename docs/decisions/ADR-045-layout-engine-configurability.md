# ADR-045 — Layout Engine Configurability and Canvas Improvements

**Date:** 2026-08-05
**Status:** Accepted

---

## Context

The `:canvas` module (ADR-043) shipped with three fixed layout engines
(`HierarchicalLayoutEngine`, `ForceDirectedLayoutEngine`, `RadialLayoutEngine`) but
exposed no way for callers to switch between them at runtime, and the canvas
composable offered no way for external code to observe or control the viewport.
Additionally, switching layouts produced an abrupt positional jump rather than
a smooth transition.

A proposal to replace the module with a new generic `:graph-canvas` library was
evaluated but rejected: it would have duplicated ~75 % of the existing module and
introduced a generics layer that the app does not need. The value-adds in the
proposal — configurable layout, viewport state extraction, and animated
transitions — were extracted and implemented directly inside the existing
`:canvas` module.

---

## Decision

### 1. `LayoutResult` — structured layout output

`computePositions()` on `GraphLayoutEngine` now returns `LayoutResult` instead of
a bare `Map`. `LayoutResult` bundles the position map with pre-computed
`minX / minY / maxX / maxY` bounding-box fields, making auto-fit calculations
cheaper and more reliable.

```kotlin
data class LayoutResult(
    val positions: Map<Long, Pair<Float, Float>>,
    val minX: Float, val minY: Float, val maxX: Float, val maxY: Float,
)
```

### 2. `GraphViewportState` — observable viewport

A new `@Stable` class holds `zoom` and `panOffset` as Compose state. Callers
obtain an instance via `rememberGraphViewportState()` and may read or set the
viewport programmatically. The `RelationGraph` composable accepts it as an
optional parameter.

### 3. `LayoutEngineType` enum + `createEngine()` factory

An enum with three values (`Hierarchical`, `ForceDirected`, `Radial`) provides a
serialisable, display-name-bearing handle for each engine. `createEngine()`
instantiates the corresponding `GraphLayoutEngine`.

### 4. Layout transition animation

`RelationGraph` animates node positions using per-node `Animatable` pairs driven
by a `LaunchedEffect(nodes)`. New nodes start at their target position (no
jump); existing nodes slide to their new positions with a `spring` easing
(`StiffnessLow`). The `pointerInput` key was changed from the full node list
to just its identity so gesture handlers are not restarted on every pan/zoom
frame.

### 5. In-app layout engine selector

Both canvas screens expose the engine choice through their control sheets
(`CanvasControlSheet` / `PersonCanvasControlSheet`) using `FilterChip` rows. The
selection is stored in a `MutableStateFlow<LayoutEngineType>` in each ViewModel
and combined into the existing filter flow before the layout computation step.

- **CategoryCanvasViewModel** defaults to `ForceDirected`; picks the
  most-connected node as root when the user switches to `Hierarchical`.
- **PersonCanvasViewModel** defaults to `Hierarchical`; the center person is
  always the root, regardless of engine.

---

## Consequences

- All callers of `computePositions()` must use `.positions` to access the map
  (a compile-time-enforced change).
- The `RelationGraph` composable gains an optional `viewportState` parameter;
  existing call sites that omit it receive a locally-remembered default.
- Users can now switch between Hierarchical, Force-Directed, and Radial layouts
  at runtime on both canvas screens; switching animates the nodes to their new
  positions.
- The `:canvas` module remains the single home for layout and rendering logic;
  no new module was created.
