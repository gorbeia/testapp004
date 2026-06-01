# ADR-021: Canvas drag-drop to create relations

**Date:** 2026-06-01
**Status:** Accepted

## Context

The category canvas displays people as nodes and relations as edges. Previously the only way to
add a relation between two people was to navigate to a person's detail screen and use the
Add Relation dialog there. Creating a relation directly from the canvas visual — by dragging one
person onto another — is a more intuitive interaction for a graph view.

## Decision

Activate drag mode on a node via long-press (500 ms). While dragging:

- The original node is rendered at 30 % opacity as a ghost at its fixed position.
- A full-opacity copy of the node follows the finger.
- Any node currently under the drag position is highlighted with a tertiary-colour ring.

On release over another node, `CategoryCanvasViewModel.openRelationDialog(fromId, toId)` is
called with the drag-source id and drop-target id. A `CanvasAddRelationDialog` appears that:

1. Shows `[drag source name] → [drop target name]` as a directional header.
2. Offers the same categorised relation-type dropdown used in `AcquaintanceDetailScreen`.
3. Derives the actual DB `fromId`/`toId` from `RelationTypeOption.isCurrentPersonFrom` — if the
   chosen type has `isCurrentPersonFrom = false`, the DB edge is reversed so that the drag source
   becomes the "to" end (e.g. selecting "Child of" from drag-source A to drag-target B stores
   B → A in the DB, meaning B is the parent).

If the finger is released over empty canvas, the drag is cancelled without any dialog.

Regular tap on a node still navigates to the person's detail screen. Pan/zoom gestures are
suppressed while a drag is active (checked via an `isDragging` flag in the gesture lambdas).

## Alternatives Considered

| Option | Why rejected |
|--------|-------------|
| Tap → detail screen to add relation | Already exists; drag-drop is a complementary, faster path for canvas users |
| Regular drag (no long-press) | Conflicts with single-finger pan; long-press clearly separates the two intents |
| Navigate to detail screen instead of inline dialog | Extra navigation breaks the canvas context |

## Consequences

- `CategoryCanvasUiState` gains five new fields: `isRelationDialogOpen`, `pendingRelationFromId`,
  `pendingRelationToId`, `pendingRelationFromName`, `pendingRelationToName`.
- `CategoryCanvasViewModel` gains `openRelationDialog`, `closeRelationDialog`,
  `addRelationFromCanvas`.
- `CanvasGraph` composable gains an `onRelationDrop` callback parameter.
- A new `CanvasAddRelationDialog` private composable is added to `CategoryCanvasScreen.kt`.
- Tap-to-navigate behaviour is unchanged.
