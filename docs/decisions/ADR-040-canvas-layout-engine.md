# ADR-040 — Canvas Layout Engine Extraction and Bézier Edge Routing

**Date:** 2026-08-04
**Status:** Accepted

---

## Context

The canvas screens had two distinct layout algorithms (radial cluster for `CategoryCanvas`, hierarchical Sugiyama-style for `PersonCanvas`) embedded directly inside their respective ViewModels. Neither algorithm was independently unit-testable.

Edge drawing used straight lines in both canvases. Two problems arose as graphs grew:

1. **Occlusion**: a straight edge between distant nodes could pass visually through intermediate nodes, making the graph unreadable.
2. **Multi-edge overlap**: when two nodes share multiple relations (e.g. both "Spouse" and "Business Partner"), their edges rendered as overlapping identical lines.

---

## Decision

### 1. Extract a `CanvasLayoutEngine` interface

```kotlin
internal interface CanvasLayoutEngine {
    fun computePositions(
        nodeIds: Set<Long>,
        edges: List<Relation>,
        rootId: Long? = null,
    ): Map<Long, Pair<Float, Float>>
}
```

Two concrete implementations replace the private layout methods that were embedded in the ViewModels:

- `RadialClusterLayoutEngine` — union-find connected components, each placed on a circle. Used by `CategoryCanvasViewModel`.
- `HierarchicalLayoutEngine` — BFS-based level assignment using `RelationType.verticalDelta`, four-pass barycentric reordering. Used by `PersonCanvasViewModel`.

Both algorithms are unchanged; only their location moved.

### 2. Bézier edge routing in the screen layer

Node bounding-box sizes are available only in the Compose UI layer (from `TextMeasurer`), so occlusion detection and path computation remain in the screen — not the ViewModel.

Before drawing each edge, the screen checks whether the straight line segment from source center to target center passes through any intermediate node's bounding box. If an intersection is found, a quadratic Bézier curve is drawn instead, with its control point displaced perpendicularly from the midpoint by `ARC_BEND = 60f` pixels.

Arrow direction for curved edges uses the tangent at `t = 1` of the quadratic Bézier (`end − controlPoint`, normalised). Edge labels are placed at `t = 0.5` (the apex of the arc) rather than the segment midpoint.

### 3. Multi-edge arc distribution

When two or more relations connect the same node pair, their edges are assigned symmetric arc offsets so they fan out visibly. The offset sign is computed in the canonical frame (smaller-ID → larger-ID) to ensure opposite-direction edges still spread to opposite sides:

```
arcSide = if (edge.fromId <= edge.toId) arcIndex else -arcIndex
```

For two edges: sides `[−1, +1]`. For three: `[−2, 0, +2]`.

---

## Consequences

- `RadialClusterLayoutEngine` and `HierarchicalLayoutEngine` are directly unit-testable without ViewModels or Hilt.
- Multi-relation node pairs render with visually distinct arcs rather than overlapping lines.
- Dense graphs with crossing edges are more readable due to automatic occlusion avoidance.
- The screen drawing code grows slightly to accommodate the arc-side pre-computation and Bézier path rendering, but remains within each canvas screen's existing `DrawScope` extension functions.
- `CanvasLayoutEngine` is `internal`; both implementations are `internal`. No public API surface is added.
