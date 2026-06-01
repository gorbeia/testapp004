# ADR-019: Add Person from Canvas with Pre-selected Category

**Status:** Accepted  
**Date:** 2026-06-01

## Context

The category canvas screen (ADR-017) was initially read-only: users had to leave the canvas,
navigate back to the main list, and use the global FAB to add a new person, with no automatic
category assignment from the canvas context.

This creates unnecessary friction when a user is browsing a category canvas and wants to add
a new person who clearly belongs to that category.

## Decision

Add a FAB (+) to `CategoryCanvasScreen`. Tapping it navigates to `AddEditAcquaintanceScreen`
with the canvas's category pre-checked in the category multi-select.

### Implementation details

- `Screen.AddEditAcquaintance` route gains an optional `preselectedCategoryId: Long`
  query parameter (default `-1`, interpreted as absent).
- `AddEditAcquaintanceViewModel` reads `preselectedCategoryId` from `SavedStateHandle`
  and initialises `selectedCategoryIds` to `{preselectedCategoryId}` for new-person flows.
  Editing an existing person is unaffected.
- `CategoryCanvasScreen` signature gains `onAddPersonClick: () -> Unit`; the FAB calls it.
- Navigation in `AppNavigation` reads the category ID from the canvas `backStackEntry`
  and passes it via `Screen.AddEditAcquaintance.createRouteWithCategory(categoryId)`.

## Consequences

- The canvas is no longer purely read-only for people: adding a person is one tap away.
- The pre-selection is a convenience default; the user can toggle any category before saving.
- Supersedes the "read-only" consequence stated in ADR-017 for people management.
