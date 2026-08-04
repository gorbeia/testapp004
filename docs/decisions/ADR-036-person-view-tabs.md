# ADR-036: Person view tabs — Detail and Canvas at equal importance

**Date:** 2026-08-04  
**Status:** Accepted

## Context

The person detail screen (`AcquaintanceDetailScreen`) and the person canvas screen (`PersonCanvasScreen`) were two separate navigation destinations. The canvas was reached via a Hub icon in the detail screen's top bar, making it visually secondary. Menus (Edit, Delete, deceased toggle) were only available on the detail screen, not on the canvas.

## Decision

Merge the two screens into a single tabbed destination:

- A `TabRow` with "Detail" and "Canvas" tabs sits below the shared `TopAppBar`.
- The shared top bar always shows **Edit**, **Delete**, and **More options** (deceased toggle), regardless of which tab is active.
- The **Filters** (Tune) button appears in the top bar only when the Canvas tab is selected (same badge behaviour as before).
- The `PersonCanvasContent` composable is the extracted canvas content without a Scaffold; it is called from the detail screen when the Canvas tab is active.
- `PersonCanvasViewModel` is instantiated alongside `AcquaintanceDetailViewModel` in the single `AcquaintanceDetail` navigation composable. Because both ViewModels read `savedStateHandle["acquaintanceId"]` and the route argument is named `acquaintanceId`, no route change is needed.
- The `PersonCanvas` navigation route and `PersonCanvasScreen` wrapper composable are removed.

## Consequences

- Tapping a relation node in the canvas tab navigates to that person's detail screen (same behaviour as before — a new entry on the back stack).
- The Hub icon is gone; switching between detail and canvas is done via tabs.
- No ViewModel logic changed; the change is entirely in the UI layer.
