# ADR-016 — Predefined relation types with perspective-aware labels

**Date:** 2026-06-01
**Status:** Accepted

---

## Context

Relations between acquaintances were stored as free-text directed edges `(fromId, toId, label)`.
This required the user to type a label every time, offered no consistency, and gave no way to
express standard relationships (spouse, parent/child, sibling, colleague, …).

Some common relations are *asymmetric*: "Parent" from A to B is the same underlying fact as
"Child" from B to A. Storing two separate rows for the same fact would be inconsistent.

---

## Decision

### Type catalogue in code, not the database

A `RelationType` data class and a `RelationTypes` singleton object live in
`model/RelationType.kt`. Each type carries:

| Field | Purpose |
|---|---|
| `key` | Stable string identifier stored in the DB (e.g. `"PARENT_CHILD"`) |
| `fromLabel` | Label shown when the current person is the *from* participant (e.g. "Parent") |
| `toLabel` | Label shown when the current person is the *to* participant (e.g. "Child") |
| `category` | `FAMILY`, `PROFESSIONAL`, or `SOCIAL` — for grouping in the picker |

Symmetric types (`SPOUSE`, `SIBLING`, `FRIEND`, …) simply have `fromLabel == toLabel`.

The type catalogue is code, not a DB table. It evolves with app updates rather than
user data migrations.

### One row per relationship, direction encodes roles

A `PARENT_CHILD` relation where A is the parent of B is stored as a single row:
`(typeKey="PARENT_CHILD", fromId=A, toId=B)`.

The label displayed depends on the perspective:
- Viewing A's profile → `fromLabel` → "Parent"
- Viewing B's profile → `toLabel` → "Child"

This is computed by the `Relation.labelFor(currentPersonId)` extension function and
resolved in the ViewModel before reaching the UI.

### Custom type

`typeKey = "CUSTOM"` with a free-text `customLabel` field covers any relation that
doesn't fit a predefined type. The old free-text behaviour is preserved for existing
records via the `MIGRATION_1_2` migration.

### UI picker

The "Add Relation" dialog offers a dropdown grouped by category. Asymmetric types appear
twice (both perspectives as separate selectable options — e.g. "Parent" and "Child").
Selecting the "to" perspective simply swaps `fromId`/`toId` before saving.

---

## Predefined types

| Key | From label | To label | Category |
|---|---|---|---|
| `SPOUSE` | Spouse | Spouse | Family |
| `PARTNER` | Partner | Partner | Family |
| `PARENT_CHILD` | Parent | Child | Family |
| `SIBLING` | Sibling | Sibling | Family |
| `GRANDPARENT_GRANDCHILD` | Grandparent | Grandchild | Family |
| `UNCLE_AUNT` | Uncle / Aunt | Nephew / Niece | Family |
| `COUSIN` | Cousin | Cousin | Family |
| `STEP_PARENT_CHILD` | Step-parent | Step-child | Family |
| `GUARDIAN` | Guardian | Ward | Family |
| `MANAGER_REPORT` | Manager | Direct report | Professional |
| `MENTOR_MENTEE` | Mentor | Mentee | Professional |
| `COLLEAGUE` | Colleague | Colleague | Professional |
| `EMPLOYER_EMPLOYEE` | Employer | Employee | Professional |
| `FRIEND` | Friend | Friend | Social |
| `NEIGHBOR` | Neighbor | Neighbor | Social |
| `ROOMMATE` | Roommate | Roommate | Social |
| `CLASSMATE` | Classmate | Classmate | Social |
| `CUSTOM` | *(free text)* | — | — |

---

## Database migration (1 → 2)

The `relations` table is recreated to replace the `label TEXT NOT NULL` column with:
- `type_key TEXT NOT NULL` — the canonical type key
- `custom_label TEXT` — populated only for `CUSTOM` relations

Existing rows are migrated with `type_key = 'CUSTOM'` and `custom_label = <old label>`.

---

## Consequences

- One row per relationship regardless of which perspective the user picked when adding it
- Label is always perspective-correct at display time with no extra data
- Adding new predefined types is a code change only (no migration needed)
- Removing a type key from code does not break existing data — the `labelFor` fallback
  returns `customLabel` for unknown keys
