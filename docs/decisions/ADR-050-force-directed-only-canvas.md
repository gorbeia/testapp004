# ADR-050: Force-Directed Layout as the Default Canvas Layout Engine

## Status

Accepted

## Date

2026-08-06

## Context

Both the category canvas and the person canvas expose an in-app layout engine
selector (Hierarchical, Force-Directed, Radial, Circular, Arc, Cluster) via a
control sheet. The person canvas previously defaulted to `HierarchicalLayoutEngine`
(ADR-038); the category canvas already defaulted to `ForceDirectedLayoutEngine`
(ADR-044).

Force-directed layout (Fruchterman–Reingold) consistently produces the most
readable graphs for both use cases: it adapts to graph structure without needing
a hand-picked root, distributes nodes evenly, and handles disconnected components
gracefully. Hierarchical layout is still useful when the user wants to inspect
family-tree or authority-chain structure explicitly.

## Decision

Change the person canvas default layout from `Hierarchical` to `ForceDirected`,
matching the category canvas. The full engine selector (all six options) remains
in both control sheets so users can still switch to any layout.

Concrete changes:

- `PersonCanvasUiState.layoutEngineType` default changes from `Hierarchical` to
  `ForceDirected`.
- `PersonCanvasViewModel.layoutEngineTypeFlow` initialised to `ForceDirected`
  (previously `Hierarchical`).
- Hierarchical positional tests in `PersonCanvasViewModelTest` are updated to call
  `vm.setLayoutEngineType(LayoutEngineType.Hierarchical)` before asserting
  positions, since those assertions are only valid for that engine.
- The "defaults to Hierarchical" test is updated to expect `ForceDirected`.

## Consequences

- Both canvas views open with force-directed layout by default.
- Users can still switch to Hierarchical (or any other engine) via the control sheet.
- The badge on the Tune icon lights up when the active engine is not the default
  `ForceDirected`, signalling that a non-default layout is in use.
