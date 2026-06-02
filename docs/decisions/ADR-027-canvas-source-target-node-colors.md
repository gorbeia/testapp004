# ADR-027 — Canvas node color distinguishes relation source from target

**Date**: 2026-06-02  
**Status**: Accepted

## Context

ADR-022 established a color scheme where canvas nodes are colored by their dominant relation
category (FAMILY → tertiary, PROFESSIONAL → primary, SOCIAL → secondary). ADR-024 added a
direct/indirect member distinction via color blending.

With the scheme as implemented, a parent and a child connected by a `PARENT_CHILD` relation
both receive exactly the same FAMILY color because the category is the same for both endpoints.
This makes the graph visually symmetric when the underlying data is directional.

## Decision

Node fill and text color are split by **role** — whether a person is a net source (`fromId`
appears more often than `toId` across all intra-category relations) or a net target (the
reverse):

| Role | Fill | Text |
|------|------|------|
| Net source (parent, manager…) | `cs.tertiary / primary / secondary` | `cs.onTertiary / onPrimary / onSecondary` |
| Net target (child, report…) | `cs.tertiaryContainer / primaryContainer / secondaryContainer` | `cs.onTertiaryContainer / onPrimaryContainer / onSecondaryContainer` |
| Balanced (`outDegree == inDegree`) | container color (same as target) | onContainer color |

Stroke color is always the accent color (`cs.tertiary / primary / secondary`) regardless of
role — it provides the category colour cue on both node types.

`isNetSource: Boolean?` is computed in `CategoryCanvasViewModel` as:
- `true` if `outDegree > inDegree`
- `false` if `inDegree > outDegree`
- `null` if equal or no relations (treated as target/container styling)

## Consequences

- Parents, managers, and other "source" roles render with a bright filled accent node.
- Children, reports, and other "target" roles render with the softer container node.
- People with equal in/out degree (e.g. symmetric spouse relations) fall back to container
  styling, which is the less prominent variant.
- The alpha-transparency approach from ADR-024 for indirect members is retained but expressed
  as a `lerp` blend toward `cs.surface` (implemented in the same commit as ADR-024's
  replacement — see commit history).
