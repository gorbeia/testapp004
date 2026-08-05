# ADR-044 — Force-directed layout for the category canvas

## Status
Accepted — 2026-08-05

## Context
The category canvas used `RadialLayoutEngine`, which placed every connected
component's nodes in a perfect circle. For large components (20+ people) this
produced a dense ring with all edges crossing through the centre, making the
graph unreadable. The user expected an organic cluster layout where highly
connected people gravitate toward each other and the overall shape reflects
the underlying social structure.

## Decision
Replace `RadialLayoutEngine` in `CategoryCanvasViewModel` with a new
`ForceDirectedLayoutEngine` that implements the Fruchterman–Reingold
spring-embedder algorithm:

- **Per-component simulation**: connected components are identified with the
  same union-find used by `RadialLayoutEngine`; each component is simulated
  independently so disconnected clusters do not attract each other.
- **Repulsion**: every pair of nodes within a component repels with force
  proportional to k²/distance, where k = 220 f (approximately one node width).
- **Attraction**: every edge attracts its two endpoints with force proportional
  to distance²/k.
- **Simulated annealing**: temperature starts at 2k and decays by 0.95 per
  iteration over 200 iterations, allowing the layout to settle.
- **Deterministic initial placement**: nodes are placed on a circle before
  the simulation begins, ensuring identical inputs always yield identical
  outputs.
- **Component grid**: after simulation, component bounding boxes are arranged
  in a row-major grid (≤ 3 per row), largest component first, exactly as
  `RadialLayoutEngine` did.

`RadialLayoutEngine` is retained in the library for the existing unit tests
and potential future use.

## Consequences
- Large connected components now form organic clusters instead of circles,
  making relation density and hub nodes visually apparent.
- The O(n²) repulsion loop is computed on the ViewModel thread; for realistic
  category sizes (< 200 nodes) the simulation completes in < 5 ms.
- Layout output is deterministic (same input → same output) because no random
  numbers are used.
- The category canvas layout is now visually distinct from the person canvas,
  which continues to use `HierarchicalLayoutEngine`.
