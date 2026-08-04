# ADR-037: Canvas-to-canvas navigation (tap person node → person canvas tab)

**Date:** 2026-08-04
**Status:** Accepted

## Context

The category canvas and person canvas both display person nodes that are tappable.
Previously, tapping a node navigated to the Detail tab of the person screen, requiring
the user to manually switch to the Canvas tab to continue exploring relations graphically.
This broke the flow for relation exploration across canvases.

## Decision

When a person node is tapped in any canvas view (category canvas or person canvas),
navigation targets the Canvas tab of the destination person screen directly, instead
of the default Detail tab.

Implementation:
- Added optional `initialTab` query parameter to the `AcquaintanceDetail` route
  (`acquaintance/{acquaintanceId}?initialTab={initialTab}`, default 0).
- Added `createRouteWithCanvasTab(id)` helper on `Screen.AcquaintanceDetail` that
  produces `initialTab=1`.
- Both canvas `onPersonClick` callbacks in `AppNavigation.kt` use `createRouteWithCanvasTab`.
- `AcquaintanceDetailScreen` accepts `initialTab: Int = 0` and seeds `selectedTab`
  from it so the Canvas tab is pre-selected on entry.

## Alternatives Considered

| Option | Why rejected |
|--------|-------------|
| Separate `PersonCanvas` route | Requires duplicating the top bar, menus, and ViewModels already owned by the detail screen |
| Always navigate to Detail tab | Forces the user to tap a second time to reach the canvas, breaking exploration flow |

## Consequences

- Tapping a person node on any canvas opens that person's relation graph directly,
  enabling fluid canvas-to-canvas exploration.
- The Detail tab is still accessible via the tab row once the screen opens.
- Navigation from the people list and other non-canvas entry points is unchanged
  (they still open the Detail tab, `initialTab` defaults to 0).
