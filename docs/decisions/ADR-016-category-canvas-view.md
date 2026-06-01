# ADR-015: Category Canvas View

**Status:** Accepted  
**Date:** 2026-05-31

## Context

Users want a visual, spatial overview of the people in a category and their relationships to each other. The existing list view shows people but not the relational structure between them.

## Decision

Add a canvas/whiteboard-style screen (`CategoryCanvasScreen`) that renders all people in a category as interactive nodes and draws directed edges for relations between them.

### Key design choices

- **Entry point:** A hub-icon button on each category row in the Categories screen.
- **Layout algorithm:** Union-Find to compute connected components; nodes within each component are arranged in a circle; components are placed in a left-to-right grid (max 3 per row).
- **Canvas rendering:** Compose `Canvas` composable with `withTransform` for pan/zoom; `TextMeasurer` for in-canvas labels.
- **Pan/zoom:** `detectTransformGestures` with proper centroid-based zoom so pinch always zooms around the pinch point.
- **Fit-to-view:** `onSizeChanged` captures the canvas pixel size; a `LaunchedEffect` computes initial zoom/pan to fit all nodes with padding.
- **Tap to navigate:** `detectTapGestures` hit-tests against node circles (virtual-space coordinates) and navigates to the person's detail screen.
- **Category scope:** Includes people from descendant categories (same logic as the list-view category filter).
- **Edges:** Only intra-category relations are drawn (both endpoints must be in the category/descendants).

## Consequences

- The canvas is read-only; adding/editing people or relations is done via the existing screens.
- Isolated people (no relations within the category) appear as separate single-node clusters.
- Very large categories may produce a dense graph; users can pan and zoom to explore.
