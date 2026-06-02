# ADR-026: sh.calvin.reorderable for Category List Drag-and-Drop

## Status
Accepted

## Date
2026-06-02

## Context
The category management screen uses a hierarchical LazyColumn with long-press drag-and-drop to
reorder items and change parent/child relationships. The original implementation managed all gesture
detection manually via `detectDragGesturesAfterLongPress`, which had several bugs:

1. **Drop target detection** used "which item does the drag center overlap", so drops in the 8 dp
   inter-item gap silently discarded the gesture.
2. **X-axis hierarchy detection** compared a cumulative drift total against a fixed threshold
   (72 dp), causing accidental reparenting on long vertical drags with slight lateral drift.
3. **No item shifting** — items stayed in their original layout slots, giving no visual
   confirmation of where the dragged item would land.

## Decision
Replace manual gesture code with `sh.calvin.reorderable:3.1.0` (Maven Central,
`sh.calvin.reorderable:reorderable`) for the Y-axis list mechanics.

The library handles:
- Smooth item-shift animation as the dragged item crosses other items' midpoints
- Correct midpoint-based insertion detection (eliminates the inter-item-gap dead zone)
- Auto-scroll when dragging near list edges

Custom code layered on top handles:
- X-axis tracking via `PointerEventPass.Initial` on the drag handle, accumulating horizontal
  displacement independently of the library's gesture consumption
- Proposed depth = `(dragStartDepth + round(dragXOffset / 24dp))` clamped to
  `[0, itemAboveDepth + 1]` — absolute position relative to indent steps, not a threshold check
- Parent derivation: walk backward in the live local tree from the item above the insertion point
  to find the first ancestor at `proposedDepth - 1`
- All writes go through the existing `viewModel.moveCategory()` at drag end; the library only
  drives the local animation state

## Consequences
- **Better**: items visually shift during drag, showing exactly where the dropped item will land
- **Better**: X-axis depth snaps to discrete indent levels based on current finger position, not
  cumulative drift
- **Better**: no dead zones — the library uses midpoint crossing, not bounding-box containment
- **Tradeoff**: a new runtime dependency (~80 kB AAR) is added
- **Tradeoff**: `reorderCategory()` is no longer called from the UI (all drops go through
  `moveCategory()`); both code paths remain in the repository layer for completeness
