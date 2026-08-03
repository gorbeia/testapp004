# ADR-035: Person canvas distance selector

**Date:** 2026-08-03
**Status:** Accepted

## Context

The person canvas view (ADR-033) shows the center person and all people directly related to
them. The category canvas already supports a distance selector (ADR-028) that expands the
graph by one or two relation hops. The same expansion is useful in the person canvas: a user
may want to see who their direct contacts know (distance 1) or to explore two hops out
(distance 2) without navigating away from the canvas.

## Decision

Add a **distance selector (0 / 1 / 2)** to the person canvas screen, surfaced via a
`ModalBottomSheet` triggered by a `Tune` icon button in the `TopAppBar`. The icon shows a
`Badge` when the distance is greater than 0.

- **Distance 0** (default): center person plus their direct contacts — unchanged from prior
  behaviour.
- **Distance 1**: distance-0 set plus every person who shares a relation with any direct
  contact but is not already visible.
- **Distance 2**: distance-1 set plus every person one further hop out.

All edges whose both endpoints are visible are shown (not only edges involving the center).

Expanded nodes are visually distinguished:

| Distance | Fill lightening | Border |
|---|---|---|
| 0 (center) | none (full colour) | solid |
| 0 (direct contact) | 25% blended to surface | solid |
| 1 | 45% blended to surface | dashed |
| 2 | 65% blended to surface | dashed |

The dashed border uses `PathEffect.dashPathEffect` on the rounded-rect `Stroke` in
`pcDrawNode`. The existing `distanceFromCategory` field on `CanvasPersonNode` carries each
node's computed hop count. `PersonCanvasUiState` gains a `relationDistance: Int` field.
`setRelationDistance(d: Int)` drives a `relationDistanceFlow: MutableStateFlow<Int>` that
is merged into the `combine` as a third source alongside acquaintances and relations.

Layout: direct contacts occupy the inner ring at radius `max(180f, n * 60f)`. Distance-1
nodes appear on a second ring at `r1 + max(150f, n1 * 50f)`, and distance-2 nodes on a third
ring at `r2 + max(130f, n2 * 40f)`.

## Alternatives Considered

| Option | Why rejected |
|--------|-------------|
| Inline chips in TopAppBar | Chips inside the app bar can overflow on narrow screens |
| Always-visible control row below TopAppBar | Wastes vertical space when not actively filtering |
| Relation-category filter alongside distance | Separate feature; keep this PR focused on distance |

## Consequences

- Large distance-2 expansions may push the auto-fit zoom to a very small level; the existing
  pan/zoom interaction handles this as it does in the category canvas.
- Dominance and net-source metrics for expanded nodes are computed only from relations between
  visible nodes, consistent with the category canvas approach.
