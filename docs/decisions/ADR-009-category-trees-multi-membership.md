# ADR-009: Category trees and multi-membership for acquaintances

**Date:** 2026-05-31
**Status:** Accepted

## Context

The acquaintance tracker categories were flat (no hierarchy) and each person
could belong to at most one category. Two limitations arose:

1. Users naturally organise contacts in overlapping groups (e.g. "Work" and
   "Family" for a colleague who became a friend, or "Work > Engineering" for a
   team sub-group).
2. A flat list of categories cannot express parent–child relationships such as
   "Work > Engineering > Backend".

## Decision

**Category trees:** Add `parentId: Long?` to `Category`. A `null` parent means
root level. The `CategoryRepository.addCategory` method accepts an optional
`parentId`. When a parent category is deleted, its direct children are orphaned
(their `parentId` is set to `null`) rather than cascade-deleted, to avoid
accidental data loss.

**Multi-membership:** Replace `Acquaintance.categoryId: Long?` with
`Acquaintance.categoryIds: Set<Long>`. The `AcquaintanceRepository` and all
ViewModels are updated accordingly. The category filter on the People list is
hierarchy-aware: selecting a category also shows people in any descendant
category (computed recursively in `AcquaintancesViewModel.descendantsAndSelf`).

## Alternatives Considered

| Option | Why rejected |
|--------|-------------|
| Keep `categoryId: Long?`, add a separate join table | More complex data model; in-memory store gains no benefit from normalisation |
| Cascade-delete children when parent deleted | Dangerous data loss; orphaning is safer and reversible by re-assigning |
| Flat multi-select (no tree) | Doesn't address the hierarchy requirement; postponing tree support would require another migration |

## Consequences

- `Acquaintance` is a breaking model change: `categoryId` → `categoryIds`.
  All callers updated in the same commit.
- The People list filter chip for a parent category now implicitly includes its
  descendants, which is the expected UX behaviour.
- The AddEditAcquaintance form switches from a single dropdown to a
  checkbox-per-category multi-select rendered in tree order.
- The Categories screen shows categories indented by depth.
- Persistent storage (future) must store `categoryIds` as a relation table.
