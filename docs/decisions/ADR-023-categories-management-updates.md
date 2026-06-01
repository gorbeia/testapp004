# ADR-023: Categories Management Updates

## Status
Accepted

## Date
2026-06-01

## Context
Three improvements were requested for the category management screen:

1. The canvas link (Hub icon) on each category row duplicated navigation already available from the main screen's category browser tab. Having it here added visual clutter without adding value.

2. Creating child categories required opening the Add dialog and manually selecting a parent from a dropdown — two extra steps. A direct "add child" button on each row makes the common case faster.

3. Categories had no persistent sort order; the display order depended on insertion order (id) from the database. Users needed a way to reorder categories manually.

## Decision

### 1. Remove canvas link from category management
Removed the Hub icon button and `onCanvasClick` callback from `CategoriesScreen`. The canvas remains accessible from the main screen's category browser tab.

### 2. Add child button per category
Added an Add (➕) icon button on each category row. Clicking it opens the `AddCategoryDialog` with the clicked category pre-selected as parent. The pre-selection can still be changed in the dialog. A new `addChildToCategory: Category?` field in `CategoriesUiState` controls which dialog variant is shown.

### 3. Drag-and-drop reordering with persistent sort order
Added a `sort_order INTEGER NOT NULL DEFAULT 0` column to the `categories` table (Room database migration 2→3). The `CategoryEntity`, `Category` model, DAO, and repository were updated to carry and persist `sortOrder`.

Each category row now shows a drag handle icon (☰). Long-pressing anywhere on the row initiates a drag using `detectDragGesturesAfterLongPress`. While dragging, the item is visually lifted (elevated + slightly translucent) and the potential drop target is highlighted (primary container color). Reordering is restricted to siblings (categories with the same `parentId`); dragging onto a category from a different parent is a no-op.

On drop, `reorderCategory(movedId, targetId)` computes the new sibling order using a remove-and-insert algorithm, then writes updated `sort_order` values back to the database. New categories are assigned `sort_order = max(siblings) + 1` so they always appear last in their group.

## Consequences
- No external drag-and-drop library is required; the implementation uses only Compose foundation gestures.
- Drag-and-drop only reorders within the same parent level. Moving a category to a different parent still requires the Edit dialog.
- The database schema bumps to version 3; devices upgrading from version 2 receive an `ALTER TABLE` migration that defaults `sort_order` to 0 (preserving existing display order by id).
