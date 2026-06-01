# ADR-022: Canvas Color Scheme Based on Relation Categories

**Date:** 2026-06-01
**Status:** Accepted

## Context

The category canvas renders every person node in the same `primaryContainer`/`primary` color
regardless of their social context.  As the graph grows, visually distinguishing nodes by the
nature of their connections helps users quickly orient themselves — family clusters look different
from work clusters, which look different from friend circles.

## Decision

Apply a two-layer relation-based color scheme using Material 3 semantic color roles:

| Relation category | Node fill | Node stroke / text | Edge |
|-------------------|-----------|--------------------|------|
| Family | `tertiaryContainer` | `tertiary` / `onTertiaryContainer` | `tertiary` |
| Professional | `primaryContainer` | `primary` / `onPrimaryContainer` | `primary` |
| Social | `secondaryContainer` | `secondary` / `onSecondaryContainer` | `secondary` |
| Custom / none | `primaryContainer` | `primary` / `onPrimaryContainer` | `outline` |

**Edges** are colored by their own relation category.

**Nodes** are colored by their *dominant* category — the category that appears most frequently
across all of the person's relations within the canvas.  Ties or no-relation nodes use the
default primary colors.

The ViewModel carries the category information:
- `CanvasRelationEdge.category: RelationCategory?` (null for custom-label relations)
- `CanvasPersonNode.dominantCategory: RelationCategory?` (null when unresolved)

Color-to-token mapping is resolved in `CanvasGraph` before entering the `Canvas` block
(composable context required for `MaterialTheme.colorScheme`).

## Alternatives Considered

| Option | Why rejected |
|--------|-------------|
| Color nodes only, keep edges neutral | Edges carry the most direct semantic signal (the relation type itself); coloring them makes the graph immediately readable without having to read labels |
| Custom hardcoded hex colors | Would break dark-mode adaptation; Material 3 roles handle light/dark automatically |
| Color by number of connections (centrality) | Centrality is a structural property, not a semantic one — less intuitive for the typical acquaintance use case |
| Add a visible legend | Deferred — the categories are already labelled in the relation-type picker; color is reinforcement, not the primary signal |

## Consequences

- The graph is visually segmented by relationship nature, aiding at-a-glance comprehension.
- Adding a new `RelationCategory` value in the future requires updating the color maps in
  `CategoryCanvasScreen.kt`.
- The drop-target highlight reuses `tertiary` (same as the family stroke color); the highlight
  ring is drawn at a larger radius so it remains visually distinct from a family node's own border.
