# ADR-018 — Combined Main Screen with Category Browser and People Search Tabs

## Status
Accepted

## Date
2026-06-01

## Context

The two primary ways to access data in the app are:
1. **Browsing categories** — navigating the category tree to orient oneself and jump to a canvas view.
2. **Searching people** — finding a specific person by name/bio or filtering by category.

Previously these lived on separate screens: `AcquaintancesListScreen` (people + category filter chips)
and `CategoryBrowseScreen` (tree explorer), with the browse screen reachable only via a toolbar icon button.
This made the category browser feel secondary, requiring an extra navigation step.

## Decision

Merge the category browser and the people search into a single **home screen** using a two-tab layout:

- **Categories tab** (default) — the full expandable/collapsible category tree with Hub icons for
  canvas navigation. This is now the primary entry point of the app.
- **People tab** — the search bar, category filter chips, and acquaintance list.

The FloatingActionButton (add person) is scoped to the People tab only.  
The overflow menu retains "Manage categories" (edit/delete) and "Check for updates".  
The standalone `CategoryBrowseScreen` route is removed from navigation; its tree logic is
inlined into the combined home screen.

## Consequences

**Positive:**
- Category browsing is a first-class, always-visible feature — no extra navigation step.
- People search remains equally accessible via the adjacent tab.
- Reduces the navigation graph by one destination.

**Negative:**
- `CategoryBrowseScreen.kt` is now an orphaned file (its logic was duplicated inline). It should
  be deleted in a follow-up cleanup, or the shared logic should be extracted to a common composable.
- Tab state is ephemeral (resets on back-stack changes if the screen is recreated).
