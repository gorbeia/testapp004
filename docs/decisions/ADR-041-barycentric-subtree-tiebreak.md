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

## Root Cause

Fernando is Esti's **spouse**, so his parents are not in the visible graph.
Both Esti and Fernando are parents of Jon and Irati.

In the tie case the nodes at the sibling level therefore have identical scores:
- Esti — bary = 0 (avg of symmetric parents), upperCount = 2, lowerCount = 2
- Fernando — bary = 0 (avg of symmetric children), upperCount = **0**, lowerCount = 2
- Iñigo — bary = 0 (avg of symmetric parents), upperCount = 2, lowerCount = 0

A single tie-breaker on lowerCount would not distinguish Esti from Fernando (both = 2).
The distinguishing property is that Fernando has **zero** connections to the parent layer.

## Decision

Add a **two-level tie-breaker** to the barycentric sort within each pass:

1. **Secondary — `upperNeighborCount` ascending**: nodes with fewer parent-layer
   connections sort first → placed furthest left. Fernando (0) < Esti (2) = Iñigo (2).
2. **Tertiary — `lowerNeighborCount` descending**: within the same upper-count group,
   nodes with more child-layer connections sort first. Resolves the case where two
   siblings share the same parents but one has children and the other does not.

```kotlin
.sortedWith(compareBy({ it.second.first }, { it.second.second }, { -it.second.third }))
//                      ↑ baryScore          ↑ upperCount            ↑ -lowerCount
```

### Effect on the Esti canvas

| Position | Node | Before | After |
|----------|------|--------|-------|
| Left | Fernando | ✗ | ✓ |
| Centre | Esti (root) | ✗ | ✓ |
| Right | Iñigo | ✓ | ✓ |

Fernando and its children (Jon, Irati) cluster on the left; Iñigo is unobstructed on
the right.

---

## Consequences

- When barycentric scores are distinct the tie-breakers never fire; existing layouts are
  unaffected.
- Nodes with no parent-layer connections (spouses, imported contacts) are naturally
  pushed to the periphery, away from the shared-parent cluster.
- Two new unit tests cover both the spouse-no-parents scenario and the all-siblings
  scenario where only the tertiary key applies.
