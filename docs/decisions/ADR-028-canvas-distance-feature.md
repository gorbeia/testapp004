# ADR-028: Canvas relation-distance expansion

**Date:** 2026-06-02
**Status:** Accepted

## Context

The canvas view shows people in a selected category (and its nested categories). This is useful
for browsing a tightly-scoped group, but the social graph often extends beyond formal category
boundaries. A person in the "Work" category may be related to family members who belong to no
category — those connections are invisible on the canvas.

A distance setting lets users expand the view incrementally to reveal first- and second-degree
relations without leaving the canvas screen.

## Decision

Add a **distance selector (0 / 1 / 2)** to the canvas screen, displayed as three `FilterChip`
buttons in a row below the `TopAppBar`.

- **Distance 0** (default): only people who belong to the canvas category or its descendants —
  unchanged from prior behaviour.
- **Distance 1**: all distance-0 people **plus** every person who shares a relation with any
  distance-0 person, regardless of category membership.
- **Distance 2**: distance-1 people **plus** every person who shares a relation with any
  distance-1 person and has not already been included.

All edges between currently-visible nodes are shown (not only intra-category edges).

Distance nodes are visually distinguished from category members:

| Membership | Fill lightening | Border |
|---|---|---|
| Distance 0, direct category member | none (full colour) | solid |
| Distance 0, child-category member only | 45% blended to surface | solid |
| Distance 1 | 60% blended to surface | dashed |
| Distance 2 | 75% blended to surface | dashed |

The dashed border uses `PathEffect.dashPathEffect` applied to the `Stroke` of the rounded-rect
node border.

A new `distanceFromCategory: Int` field is added to `CanvasPersonNode` (0, 1, or 2). The
`CategoryCanvasUiState` gains a `relationDistance: Int` field mirroring the current selector
value. `setRelationDistance(d: Int)` on the ViewModel drives `_relationDistance: MutableStateFlow`
which is merged into the existing `combine` flow as a fourth source.

## Alternatives Considered

| Option | Why rejected |
|--------|-------------|
| Slider (0–2) | FilterChips show all three options simultaneously with clearer tap targets |
| TopAppBar trailing actions | Cramped alongside the back button; chips need labels |
| Overlay floating on canvas | Harder to discover; chips may occlude nodes |

## Consequences

- Expanding to distance 2 on a highly-connected dataset can surface a large number of nodes;
  the layout algorithm and auto-fit handle this automatically but may result in a very small
  zoom level.
- The dominantCategory and isNetSource fields on distance-1/2 nodes reflect only relations to
  other currently-visible nodes, not all their relations globally.
