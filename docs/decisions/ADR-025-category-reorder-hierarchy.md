# ADR-025: Category drag-drop hierarchy reordering

## Status
Accepted

## Date
2026-06-01

## Context
The category manager supported sibling reordering via drag-drop (ADR-023), but had no way to change a category's parent through drag. The only path to reparent a category was through the Edit dialog's parent dropdown. Users expected to be able to drag a category under another to make it a child, and drag it left to promote it to a higher level — the same interaction model used by popular list/outline apps.

## Decision
Extend the existing long-press drag gesture with an X-axis dimension:

- **Drag right (X > 72 dp threshold):** the item under the drag center becomes the proposed new parent; the dragged card snaps visually to the new indentation depth and the proposed parent is highlighted in `tertiaryContainer` color.
- **Drag left (X < –72 dp threshold):** the category is promoted one level — its new parent becomes its current parent's parent (or root if already at depth 1).
- **Drag within threshold (X neutral):** existing sibling reorder behaviour is preserved unchanged.

A new `moveCategory(movedId, newParentId, targetPositionId)` method is added to `CategoryRepository`, `RoomCategoryRepository`, and `FakeCategoryRepository`. It atomically:
1. Guards against self-parenting and cycles (checks the ancestor chain).
2. Updates `parent_id` in the database when the parent changes.
3. Rewrites `sort_order` for both the old and new sibling groups.

The `CategoriesViewModel` gains `moveCategory()` which delegates to the repository. The UI calls `reorderCategory` for same-parent drops and `moveCategory` for cross-parent drops.

## Consequences
- Reparenting via drag is now possible without opening the Edit dialog.
- The edit dialog's parent dropdown is unchanged and remains the explicit path for precise reparenting.
- The 72 dp X threshold requires a clear intentional horizontal gesture, reducing accidental reparents during vertical reordering.
- `moveCategory` supersedes the need for separate promote/demote operations; both are expressed as a single move to a new parent.
