# ADR-038: Person Canvas Hierarchical Layout

## Status
Accepted

## Context

The person canvas was using a pure radial layout: the center person at the
origin, direct relations on a first ring, distance-1 nodes on a second ring,
distance-2 nodes on a third ring. Every ring used uniform angular spacing with
no regard for relationship semantics. The result was visually messy for
family-type graphs: many edges crossed each other, relation-type labels
overlapped, and the layout gave no visual hint about generational hierarchy
(parents and children were scattered around the same ring as siblings and
spouses).

## Decision

Replace the radial layout in `PersonCanvasViewModel` with a **hierarchical
(layered) layout** driven by the semantic direction of each relation type:

| Relation type | Vertical delta |
|---|---|
| `PARENT_CHILD`, `STEP_PARENT_CHILD`, `UNCLE_AUNT`, `GUARDIAN` | +1 (fromId one generation above toId) |
| `GRANDPARENT_GRANDCHILD` | +2 |
| `MANAGER_REPORT`, `MENTOR_MENTEE`, `EMPLOYER_EMPLOYEE` | +1 |
| All peer relations (SIBLING, SPOUSE, PARTNER, FRIEND, COLLEAGUE, …) | 0 |

**Algorithm:**

1. BFS from the center person, propagating generation levels through visible
   relations using the vertical-delta table. Nodes not reached by any
   hierarchical edge default to level 0.
2. Group nodes by level. Place them on evenly-spaced horizontal rows (170 px
   apart vertically, 220 px between nodes on the same row), each row centered
   at x = 0.
3. Run 4 passes of barycentric reordering (alternating top-down and
   bottom-up): within each level, sort nodes by the average x of their
   neighbours in adjacent levels. This dramatically reduces edge crossings.
4. Translate all positions so the center person lands at the origin.

**Edge label improvement:** Labels are now offset perpendicular to the edge
direction and drawn over a semi-transparent background pill, preventing them
from sitting on top of the line and overlapping adjacent labels.

## Consequences

- Family graphs show a natural generational hierarchy: grandparents at the
  top, parents one row down, the center person in the middle, children below.
- Sibling and peer nodes land on the same horizontal row as the center.
- Edge crossings are greatly reduced compared to the radial layout.
- Professional hierarchy (manager above direct report) is also reflected.
- For graphs with only peer relations (all friends/colleagues), nodes appear
  in a single horizontal row — the barycentric pass still spaces them
  evenly.
- The center person is always at (0, 0); the auto-zoom then fits the whole
  graph on screen as before.
