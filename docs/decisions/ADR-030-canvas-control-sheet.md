# ADR-030: Canvas control sheet — distance and relation-category filter in a bottom sheet

## Status
Accepted

## Date
2026-06-06

## Context
The canvas distance selector (0 / 1 / 2) was a permanent row at the top of the canvas screen,
consuming vertical space at all times even though it is only useful during exploration. A new
relation-category filter (Family / Professional / Social) needed to be added as a companion control,
but placing both rows permanently at the top would make the chrome too dominant relative to the canvas.

## Decision
Move both controls into a `ModalBottomSheet` triggered by a "tune" (`Icons.Default.Tune`) icon in the
TopAppBar. A `BadgedBox`/`Badge` dot on the icon signals when any filter is active. The sheet contains:

- **Distance** — `FilterChip` group (0 / 1 / 2), single-select
- **Relations** — `FilterChip` group (Family / Professional / Social), multi-select toggle

The relation-category filter restricts both edge visibility and distance-expansion: only relations
whose `RelationCategory` is in the active filter set are followed when computing distance nodes and
rendered as edges. Direct category members are never affected by the filter.

`ViewModel` changes:
- New `relationCategoryFilter: Set<RelationCategory>` field in `CategoryCanvasUiState`
- `private val relationCategoryFilterFlow` combined with the existing `relationDistanceFlow` into a
  `filtersFlow` (private `CanvasFilters` data class) to keep the main `combine()` at four flows
- `fun toggleRelationCategoryFilter(category: RelationCategory)` public method

`UI` changes:
- Distance row removed from the screen body
- `Column` wrapper removed (canvas `Box` now fills the scaffold content directly)
- `actions` slot in `TopAppBar` holds a badged tune `IconButton`
- Private `CanvasControlSheet` composable manages the `ModalBottomSheet`

## Consequences
- Canvas area is fully reclaimed — no permanent chrome below the app bar
- One icon + badge replaces the always-visible distance row
- Both discovery controls are accessible from a single, consistent entry point
- The category filter is coherent with distance: enabling FAMILY-only at distance 1 shows only
  family-connected neighbours, never social or professional ones
