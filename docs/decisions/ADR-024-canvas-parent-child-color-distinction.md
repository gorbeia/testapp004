# ADR-024: Canvas Node Color Distinction for Parent vs Child Category Membership

**Date:** 2026-06-01
**Status:** Accepted

## Context

The category canvas includes people from the viewed category AND all of its descendant
categories (via `descendantsAndSelf`).  Under ADR-022 every node renders with the same
saturation regardless of which category level the person actually belongs to, so there is
no visual cue that distinguishes a direct member of the root category from someone who is
only a member of a child category.

## Decision

Add a `isDirectMember: Boolean` field to `CanvasPersonNode`.  It is `true` when the
person's `categoryIds` contains the root `categoryId` of the canvas, and `false`
when the person belongs only to a descendant category.

Node fill and stroke colors are then rendered in two tiers:

| Membership | Fill | Stroke |
|------------|------|--------|
| Direct (parent category) | Full ADR-022 container color | Full ADR-022 stroke color |
| Indirect (child category only) | `lerp(containerColor, surface, 0.45f)` | `lerp(strokeColor, surface, 0.45f)` |

`lerp` is used instead of alpha because canvas compositing with a transparent fill over the
background produces the same visual result as blending toward the surface color but is
harder to reason about.  The 45 % blend toward surface gives a clearly lighter but
same-hue appearance that is still distinguishable from the background.

Text color is unchanged because the `onContainer` tokens remain readable on both the
full and the lightened fill.

## Alternatives Considered

| Option | Why rejected |
|--------|-------------|
| Different stroke width for indirect members | Harder to perceive at small node sizes |
| Dashed border for indirect members | Adds visual noise; the goal is subtle, not distracting |
| Separate legend entry | Deferred — the visual difference is intuitive enough without a legend |

## Consequences

- Users can visually distinguish people directly in the viewed category from people
  pulled in through child categories.
- Any future change to the blend ratio or the distinction logic is isolated to the
  two `nodeFill` / `nodeStroke` helpers in `CategoryCanvasScreen.kt` and the
  `isDirectMember` assignment in `CategoryCanvasViewModel.kt`.
