# ADR-031: Foldable category structure in the Categories management screen

## Status
Accepted

## Date
2026-08-03

## Context
The Categories management screen displays the full category tree as a flat, indented list.
Once a user has accumulated many categories (especially with multi-level hierarchies), the
list becomes long and hard to navigate: every category is always visible and there is no
way to hide subtrees that are not currently of interest.

## Decision
Add expand/collapse behaviour to the Categories management screen.

- Each category that has at least one child displays a chevron icon button (`KeyboardArrowDown`
  when expanded, `KeyboardArrowRight` when collapsed) placed between the drag handle and the
  category name.
- Leaf categories (no children) show a fixed-width spacer in place of the chevron to keep the
  name column aligned.
- Collapse state is stored as `collapsedCategories: Set<Long>` in `CategoriesUiState`. It is
  session-local: it resets when the screen is left and is not persisted to the database.
- `buildCategoryTree` accepts the `collapsed` set and skips the subtree of any collapsed node,
  so the `LazyColumn` only ever contains visible items. This also means drag-and-drop operates
  only on visible items, which is the expected behaviour.
- `CategoriesViewModel.toggleCollapsed(categoryId)` XORs the ID into/out of the set.

## Consequences
- Long category trees can be navigated efficiently by collapsing unneeded branches.
- Drag-and-drop reordering continues to work unchanged on the visible items.
- Collapsed state is not persisted (intentional — it is ephemeral presentation state).
- Leaf items get a 48 dp spacer to preserve name-column alignment when mixed with parent items.
