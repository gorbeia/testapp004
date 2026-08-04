# ADR-040: Person Canvas Edge Label Deduplication

**Date:** 2026-08-04
**Status:** Accepted

## Context

The person canvas hierarchical layout (ADR-038) places nodes in rows by
generation. When multiple edges share the same label and connect the same
pair of rows — for example, two parents both drawn as "Parent" to the
center row — each edge independently rendered its label pill at the
midpoint of that edge. With several parents or children, the label pills
stacked on top of one another, making the canvas unreadable.

## Decision

Replace per-edge label rendering with a grouped, centroid-based approach:

1. Remove the label-drawing code from `pcDrawEdge`. The function now only
   draws the line and optional arrowhead.
2. Add `pcDrawEdgeLabels(edges, nodeMap, labelColor, labelBgColor, textMeasurer)`
   called once per frame, after all edges are drawn and before nodes are drawn.
3. Inside `pcDrawEdgeLabels`, group edges by a `PcEdgeLabelKey(label, fromRow, toRow)`
   where `fromRow = roundToInt(node.y / PC_LAYER_H)`. All edges that share
   the same label and connect the same pair of rows map to the same key.
4. For each key, compute the centroid of all per-edge midpoints and draw
   exactly one label pill there.

For the motivating case of two parents of Esti: both edges have label
"Parent", fromRow = -1, toRow = 0 → single key → centroid at (0, -85) →
one "Parent" pill instead of six stacked ones.

## Alternatives Considered

| Option | Why rejected |
|--------|-------------|
| External graph layout library (JGraphT, Cytoscape.js/WebView) | No mature native Compose library exists; WebView approach is a major architectural change with no benefit for label deduplication alone |
| Offset each label perpendicular to its edge | Does not eliminate duplicates; still O(n) overlapping pills for common relation types |
| Suppress all labels | Loses useful relationship-type information |

## Consequences

- The visual result is one readable label per (label, fromRow, toRow)
  triple regardless of how many parallel edges share that triple.
- `pcDrawEdge` signature is simplified (no text-related parameters).
- The grouping is row-pair based: edges between different row pairs that
  happen to share a label are still given separate labels, which is correct
  (e.g. "Parent" from row -1→0 and "Parent" from row -2→-1 are distinct
  relationships in the graph).
- No new dependencies introduced.
