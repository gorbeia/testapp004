# ADR-049: Canvas visualization optimizations (minimum zoom, layer height, component grid width)

**Date:** 2026-08-05
**Status:** Accepted

## Context

Two usability problems were observed on the canvas screens:

1. **Unreadable Arc/wide layouts.** The Arc layout engine places all nodes at `y=0`.
   When the graph contains many nodes the content bounding box becomes very wide but
   almost zero in height. The fit-to-screen formula
   `min(screenW/contentW, screenH/contentH)` then produces a zoom well below 0.1,
   rendering nodes as a thin strip of 1–2 pixels — effectively invisible. The previous
   minimum zoom floor of `0.1f` did not prevent this.

2. **Hierarchical trees taller than necessary.** The `layerHeight = 170f` constant in
   `HierarchicalLayoutEngine` created excessive whitespace between levels, causing deep
   trees to extend beyond the screen and require zooming out to the point where node
   labels became hard to read.

3. **Narrow component grids.** Both `placeComponentsAsCircles` (used by Cluster and
   Radial fallback layouts) and `ForceDirectedLayoutEngine.arrangeComponents` used
   `maxPerRow = 3`, producing tall grids of components when many disconnected people
   are present in a category.

## Decision

Three parameter changes in the `:canvas` module:

| File | Change | Old → New |
|------|--------|-----------|
| `RelationGraph.kt` | Minimum viewport zoom in fit-to-screen | `0.1f` → `0.35f` |
| `GraphLayoutEngine.kt` (`HierarchicalLayoutEngine`) | Vertical gap between layers | `170f` → `130f` |
| `GraphLayoutEngine.kt` (`placeComponentsAsCircles`, `ForceDirectedLayoutEngine`) | Components per grid row | `3` → `5` |

**Minimum zoom (0.35f):** At this floor a node of height `2 × NODE_HALF_H = 52` layout
units renders at `≥18 px` on screen. For very wide content (e.g. Arc layout with 30+
nodes) only the central portion of the graph is visible on first load, but nodes are
legible and the user can pan to reveal the rest. The previous floor of `0.1f` allowed
nodes to shrink to ~5 px — too small to tap or read.

**Layer height (130f):** Reduces the vertical span of a 10-level tree from 1700 to
1300 layout units, bringing it within the screen height of a typical phone at zoom ≈ 1.
Node half-height is 26 units, so there remains `130 − 52 = 78` units of whitespace
between node edges — sufficient to keep levels visually distinct and edge labels readable.

**Components per row (5):** A grid of isolated single-person nodes is now wider and
shorter (5 per row instead of 3), producing a more square layout rather than a tall
narrow column.

## Alternatives Considered

| Option | Why rejected |
|--------|-------------|
| Remove Arc layout for category canvas | Arc layout is a valid user choice; better to make it degrade gracefully |
| Re-centre viewport on root/median node when content is too wide | More complex; centering on bounding-box midpoint already shows the most relevant part for most layouts |
| Increase minimum zoom to 0.5f or higher | Would clip even moderately large graphs; 0.35f is a sensible lower bound |
| Keep layerHeight at 170f, reduce padding instead | Padding reduction clips edge labels at the bounding box edge |

## Consequences

- Wide-content layouts (Arc, many isolated nodes) are always legible on initial load,
  at the cost of not fitting the full graph on screen — the user must pan.
- Hierarchical trees up to ~15 levels now fit on a typical phone screen without zooming out below 1×.
- Component grids for category canvases with many unconnected people are more compact.
- The `arc layout places all nodes on same horizontal line` test is unaffected (the
  ArcLayoutEngine itself is unchanged).
- The `hierarchical layout disconnected nodes default to same level as root` test is
  unaffected (only `layerHeight` changed, not level assignment).
