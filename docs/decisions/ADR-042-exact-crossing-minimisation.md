# ADR-042 — Exact Crossing Minimisation in Hierarchical Layout

**Date:** 2026-08-04
**Status:** Accepted

---

## Context

ADR-041 introduced a two-level tie-breaker (upperNeighborCount ASC, lowerNeighborCount DESC)
on top of the barycentric heuristic to fix the Fernando/Esti/Iñigo layout. That change
correctly resolves the symmetric-score case but leaves the barycentric heuristic as the
primary driver. The heuristic is a proxy for crossing minimisation — it does not guarantee
a minimum-crossing ordering.

A secondary problem was that BFS discovery order was the implicit final tie-breaker, making
the layout non-deterministic across graph mutations (insertion/deletion order affects BFS).

---

## Decision

Replace the 4-pass barycentric sort with **exact permutation crossing minimisation** for
small layers, falling back gracefully for larger ones.

### Algorithm

Within each pass (4 passes, alternating top-down/bottom-up):

| Layer size | Strategy |
|------------|----------|
| N ≤ 6 | Exhaustive permutation search (Heap's algorithm, max 720 permutations). Picks the permutation with fewest crossings; ties broken by node-ID string order (deterministic). |
| 7 ≤ N ≤ 8 | Adjacent-swap (bubble-sort-style) with exact crossing counts. |
| N > 8 | Barycentric sort with 4-tier key: baryScore ASC → upperNeighborCount ASC → −lowerNeighborCount ASC → nodeId ASC. Then one pass of adjacent-swap. |

Crossings between the permuted layer and each adjacent fixed layer are counted separately
and summed, avoiding spurious interactions between up-edges and down-edges.

### Crossing count

```kotlin
private fun countCrossings(permuted: List<Long>, fixed: List<Long>, edges: List<Relation>): Int
```

Builds position-index maps for each layer, extracts cross-layer edges, then checks each
pair of edges for order inversion: `(p1 - p2) * (f1 - f2) < 0`.

### Initial placement (before passes)

Nodes within each level are sorted by the same 4-tier key before the first pass so that
the first optimisation pass starts from a reasonable state.

### Dynamic node spacing

`nodeSpacing = 220f * (4 / maxNodesInAnyLayer).coerceIn(0.7, 1.3)`

Scales spacing down for wide graphs and up for narrow ones, keeping the canvas readable
without wasting screen space.

### Determinism

The `nodeId: Long` final tier guarantees identical output for identical inputs regardless
of insertion order, set iteration order, or BFS traversal variation.

---

## Consequences

- Layouts with ≤ 6 nodes per layer are **provably optimal** (minimum crossings).
- Layouts of 7–8 nodes per layer converge to a local optimum in bounded time.
- For N > 8 the barycentric heuristic is still used but is now seeded more intelligently
  and refined by one adjacent-swap pass.
- The barycentric tie-breaker from ADR-041 (upperNeighborCount / lowerNeighborCount) is
  preserved as part of the 4-tier fallback key; the exact minimiser supersedes it for
  small layers but the tie-breaker still fires in the large-layer fallback path.
- Unit tests cover: crossing minimisation correctness, determinism, the spouse-no-parents
  scenario (ADR-041), and the sibling-with-children scenario (ADR-041).
