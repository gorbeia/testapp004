# ADR-043 — Android canvas shared library

**Status:** Accepted  
**Date:** 2026-08-05

---

## Context

The project had two canvas implementations — `CategoryCanvasScreen` and `PersonCanvasScreen` — sharing the same pan/zoom/drag gesture pattern, arrowhead and Bézier-edge drawing, and node-rendering logic through duplicated, prefixed functions (`drawNode`/`pcDrawNode`, `drawEdge`/`pcDrawEdge`, etc.). Layout algorithms (`RadialClusterLayoutEngine`, `HierarchicalLayoutEngine`) were marked `internal` in the app's `viewmodel` package.

The goal is to extract the reusable canvas machinery into a standalone Gradle module that can eventually be published as a library while keeping the app-specific data mapping (relation categories, colour derivation, ViewModel wiring) clearly on the app side.

---

## Decision

Extract a `:canvas` Android library module (`com.example.canvasgraph`) containing:

| Component | Description |
|-----------|-------------|
| `GraphNode` + `NodeStyle` | Fully-resolved node model. All colours are pre-computed by the caller; `NodeStyle` carries `ringColor` for optional centre-node highlights. |
| `GraphEdge` + `EdgeStyle` | Fully-resolved edge model. `EdgeStyle.labelBgColor` is optional — when set, a rounded rect is drawn behind the label. |
| `LayoutEdge` | Generic edge for layout computation, carrying `verticalWeight` instead of an app-specific relation type key. |
| `GraphLayoutEngine` interface | `computePositions(nodeIds, edges, rootId?)`. |
| `RadialLayoutEngine` | Cluster-based radial layout (renamed from `RadialClusterLayoutEngine`). |
| `HierarchicalLayoutEngine` | Layer-based hierarchical layout with crossing minimisation. |
| `RelationGraph` composable | Single pan/zoom/drag-to-relate composable that replaces both `CanvasGraph` and `PersonCanvasGraph`. Accepts `dropTargetHighlightColor` for graph-level interaction styling. |

### Boundary rules

- The library has **no knowledge** of `RelationCategory`, `Acquaintance`, `Relation`, `RelationTypes`, or any DI framework.
- The app maps `List<Relation>` → `List<LayoutEdge>` (supplying `verticalWeight = RelationTypes.findByKey(typeKey)?.verticalDelta`) before calling the layout engine.
- The app resolves all colours from `MaterialTheme.colorScheme` and constructs `NodeStyle`/`EdgeStyle` before passing nodes and edges to `RelationGraph`.
- `CanvasPersonNode` and `CanvasRelationEdge` remain app-internal ViewModel output types; the composable converts them to `GraphNode`/`GraphEdge` using `remember`-keyed colour resolution.

### Flexible styling

Callers control every visual aspect through `NodeStyle` (fill, stroke, text, dashed border, optional ring) and `EdgeStyle` (edge colour, label colour, optional label background). The library imposes no colour constants beyond `dropTargetHighlightColor`, which defaults to `MaterialTheme.colorScheme.tertiary` and can be overridden per call site.

---

## Consequences

- Both canvas screens now share a single `RelationGraph` composable; the duplicated draw helpers and gesture code are gone from the app module.
- The layout engines are public and testable from any module; `CanvasLayoutEngineTest` migrated from `Relation` to `LayoutEdge`.
- Adding a third canvas view requires only colour-resolution logic in the new screen — no gesture or draw code to copy.
- When the library is eventually published, the package (`com.example.canvasgraph`) and group ID will change; call sites in the app update their import only.
