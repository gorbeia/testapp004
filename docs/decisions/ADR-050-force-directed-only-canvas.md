# ADR-050: Force-Directed Layout as the Sole Canvas Layout Engine

## Status

Accepted

## Date

2026-08-06

## Context

Both the category canvas and the person canvas previously exposed an in-app layout engine
selector (Hierarchical, Force-Directed, Radial, Circular, Arc, Cluster) via a control sheet.
The selector added UI surface and code complexity while force-directed (Fruchterman–Reingold)
consistently produced the most readable graphs for both use cases:

- **Category canvas**: already defaulted to ForceDirected since ADR-044.
- **Person canvas**: defaulted to Hierarchical since ADR-038, but the hierarchical placement
  depended on relation `verticalDelta` metadata that is not always meaningful.

The engine selector is also hard to discover and rarely used in practice; it was added
speculatively in ADR-045 and has not proven its value.

## Decision

Remove the layout engine selector from both canvas control sheets. Both the category canvas
and the person canvas use `ForceDirectedLayoutEngine` exclusively, hardcoded in their
respective ViewModels.

Concrete changes:

- `PersonCanvasUiState` and `CategoryCanvasUiState` drop the `layoutEngineType` field.
- `PersonCanvasViewModel.setLayoutEngineType()` and `CategoryCanvasViewModel.setLayoutEngineType()`
  are removed.
- The "Layout" section (FlowRow of engine chips) is removed from both `CanvasControlSheet` and
  `PersonCanvasControlSheet`.
- `forceArcs` is hardcoded to `false` on both `RelationGraph` calls (Arc layout is no longer
  selectable).
- The six other engine classes (`HierarchicalLayoutEngine`, `RadialLayoutEngine`, etc.) remain
  in the `:canvas` library for potential future use.

## Consequences

- **Simpler UI**: the control sheet on both canvases shows only Distance (and, on category
  canvas, Relations filter). No engine chip row.
- **Predictable behaviour**: users always see a force-directed graph; there is no risk of the
  graph switching to an unfamiliar layout.
- **Removed tests**: position-assertion tests that relied on hierarchical placement guarantees
  are deleted; layout-engine-selector ViewModel tests are deleted. Fundamental ForceDirected
  engine tests remain in `CanvasLayoutEngineTest`.
- **Irreversible for now**: re-adding the selector would require new UI and ViewModel changes,
  treated as a fresh feature.
