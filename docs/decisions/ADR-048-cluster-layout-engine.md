# ADR-048 — Cluster Layout Engine (Component-Circle Layout)

**Date:** 2026-08-05
**Status:** Accepted

---

## Context

The original category canvas arranged nodes in multiple circles, one per connected
component, with the circles tiled in a grid by descending component size. This layout
was replaced by force-directed in ADR-044 and was no longer accessible after that.

Users found the component-circle style visually useful — it immediately shows which
people are connected versus isolated, and the circle-per-group visual metaphor is
easy to read at a glance. The representation is commonly called a **cluster layout**
(also known as "component-circle" or "connected-component circles").

---

## Decision

### 1. Extract shared helpers to file scope

`findConnectedComponents` and `placeComponentsAsCircles` (previously private methods
of `RadialLayoutEngine`) are extracted to private file-level functions so that both
`RadialLayoutEngine` (fallback path) and the new `ClusterLayoutEngine` can share them
without duplication.

### 2. `ClusterLayoutEngine` — new engine

```kotlin
class ClusterLayoutEngine : GraphLayoutEngine {
    override fun computePositions(
        nodeIds: Set<Long>,
        edges: List<LayoutEdge>,
        rootId: Long?,
    ): LayoutResult {
        if (nodeIds.isEmpty()) return LayoutResult.Empty
        val components = findConnectedComponents(nodeIds.toList(), edges)
        return placeComponentsAsCircles(components).toLayoutResult()
    }
}
```

`rootId` is intentionally ignored — there is no ego node in a cluster layout.

### 3. `LayoutEngineType.Cluster`

A new `Cluster("Cluster")` value is added to the enum; `createEngine()` maps it to
`ClusterLayoutEngine()`. Both canvas control sheets already use `FlowRow`, so the
sixth chip renders without any layout changes.

---

## Consequences

- The original "several circles for related items" view is available again as an
  explicit engine choice ("Cluster") alongside the other five options.
- No ViewModel changes are required; `rootId` is simply unused by `ClusterLayoutEngine`.
- `ForceDirectedLayoutEngine` retains its own private `findConnectedComponents` copy
  (it is used differently, paired with the Fruchterman–Reingold simulation); unifying
  it is deferred to a future refactor if needed.
