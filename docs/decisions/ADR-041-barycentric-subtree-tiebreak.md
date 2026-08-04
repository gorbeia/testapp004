# ADR-041 — Barycentric Tie-Breaking by Lower-Level Neighbor Count

**Date:** 2026-08-04
**Status:** Accepted

---

## Context

The `HierarchicalLayoutEngine` uses a four-pass barycentric reordering algorithm to minimise
edge crossings in the person canvas. Each node at a given level is scored by the average
x-position of its cross-level neighbours; nodes are then sorted by that score.

When two parents (e.g. Juanje and Maite) are **both** parents of **all** siblings at the
same level (e.g. Esti, Fernando, Iñigo), every sibling node receives an identical
barycentric score of 0. The sort becomes a no-op and BFS discovery order is preserved.
BFS visits the root first, so the root ends up leftmost and a sibling with children
(Fernando) ends up in the centre — separating Iñigo from its clearest parent edges and
cluttering the canvas.

User observation (screenshot, 2026-08-04):

> "Wouldn't make more sense to draw Fernando, Jon and Irati to the left so the
> relation with Iñigo remains unobstructed?"

---

## Decision

Add a **secondary sort key** to the barycentric comparison: when two nodes have the same
primary barycentric score, sort by their count of lower-level neighbours **descending**
(more children → sorted first → placed further left).

```kotlin
.sortedWith(compareBy({ it.second }, { -it.third }))
//                      ↑ baryScore       ↑ -lowerNeighborCount
```

`lowerNeighborCount` for node `id` at `level` is the number of visible-relation endpoints
whose assigned level is strictly less than `level` (i.e., rendered below).

### Effect on the Esti canvas

| Position | Node | Before | After |
|----------|------|--------|-------|
| Left | Fernando | ✗ | ✓ |
| Centre | Esti (root) | ✗ (was at left after centering) | ✓ |
| Right | Iñigo | ✓ | ✓ |

Fernando and its children (Jon, Irati) cluster on the left; Iñigo is unobstructed on
the right.

---

## Consequences

- When barycentric scores are distinct the secondary key never fires; existing layouts
  are unaffected.
- The tie-breaking generalises: any node with more children is pulled left relative to
  nodes with fewer children at the same level.
- One new unit test (`CanvasLayoutEngineTest`) covers the tie-breaking with the exact
  family structure that triggered the bug.
