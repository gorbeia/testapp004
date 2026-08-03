# ADR-033 — Person Canvas View

## Status

Accepted — 2026-08-03

## Context

The existing canvas view is scoped to a category: it shows all people in a category and the
relations between them. There was no way to get a quick visual overview of a single person's
relations — users had to scroll through the relations list on the detail screen and mentally
construct the graph themselves.

## Decision

Add a person-centric canvas screen (`PersonCanvasScreen`) reachable from a new Hub icon button
in the `AcquaintanceDetailScreen` top app bar.

The canvas places the focus person at the centre of the viewport and arranges all directly related
people in a radial ring around them. Ring radius scales with the number of related people so nodes
do not overlap. Edges are labelled and arrowed using the same conventions as the category canvas
(asymmetric relations get an arrowhead, symmetric ones do not).

The focus person's node is visually distinguished by a semi-transparent accent ring drawn behind
it and by having full-brightness fill (`isDirectMember = true`) while surrounding nodes use the
slightly-dimmed variant (`isDirectMember = false`). This reuses the existing color system from
`CanvasPersonNode` without adding new fields.

The screen supports the same drag-drop relation-creation gesture as the category canvas: long-press
a node and drag onto another to open the `PersonCanvasAddRelationDialog`. Tapping any node navigates
to that person's detail screen.

Implementation choices:
- `PersonCanvasViewModel` takes `acquaintanceId` from `SavedStateHandle` (same pattern as
  `AcquaintanceDetailViewModel` and `CategoryCanvasViewModel`).
- The ViewModel reuses `CanvasPersonNode` and `CanvasRelationEdge` from `CategoryCanvasViewModel`.
- The canvas drawing helpers (`pcDrawNode`, `pcDrawEdge`, `pcRectBorderPoint`) are private to
  `PersonCanvasScreen` to avoid coupling the two canvas screens through a shared file.
- No distance/filter controls: the person canvas always shows only direct relations.
- Navigation route: `person_canvas/{acquaintanceId}` (follows the existing `Long`-argument pattern).

## Consequences

- Users can open the relation canvas for any person directly from the detail screen.
- The canvas is read-only for navigation (tap → detail) and write-enabled for new relations
  (drag-drop → dialog → persisted via `RelationRepository`).
- No new Room migration, no new repository method, no new model field required.
