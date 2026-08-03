# ADR-034 — Move Deceased Toggle to Overflow Menu

## Status

Accepted — 2026-08-03

## Context

ADR-032 placed the deceased toggle as a `Switch` inside the Bio card on the detail screen. This made
"Mark as deceased" a prominent, always-visible control alongside everyday content like the bio and
birthday — even though toggling deceased status is a rare, one-time action for most people.

## Decision

Supersedes the UI affordance described in ADR-032 (the Switch inside the Bio card).

The deceased toggle moves to a `DropdownMenu` behind the `MoreVert` overflow icon in the detail
screen's top app bar. The menu item reads "Mark as deceased" for living people and "Mark as alive"
for deceased people. This groups the action with other infrequent settings rather than surfacing it
in the main content area.

The Bio card retains a read-only "Deceased" label (error-colour `labelMedium` text) at the top when
`isDeceased = true`, so the status remains visible at a glance without an interactive affordance in
the card.

## Consequences

- No data model, repository, or ViewModel changes — `toggleDeceased()` is called from the menu item
  the same way it was called from the Switch `onCheckedChange` callback.
- The overflow menu is a natural extension point for future infrequent settings on the detail screen.
- Existing unit tests for `toggleDeceased()` are unaffected.
