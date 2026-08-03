# ADR-032 — Mark Deceased People

## Status

Accepted — 2026-08-03

## Context

Users track acquaintances over time. Some of those people pass away. The app had no way to record this
fact, so deceased acquaintances were indistinguishable from living ones.

## Decision

Add a boolean `isDeceased` flag to the `Acquaintance` domain model, persisted as an `is_deceased`
integer column (0/1) in the `acquaintances` Room table (schema version 5, migration 4→5).

The flag is toggled exclusively from the person detail screen via a Switch control inside the Bio card.
The add/edit screen carries the flag through unchanged so that editing a deceased person's name or
categories does not reset the status.

The people list renders a `†` (dagger) suffix after the name and applies a muted `onSurfaceVariant`
colour for deceased entries, following the typographic convention for indicating deceased persons.

## Consequences

- Existing installs receive a non-destructive `ALTER TABLE` migration; all rows default to `is_deceased = 0`.
- No breaking change to `AcquaintanceRepository` or existing callers — the field carries a default value.
- The canvas and relation views are unaffected; deceased status is a biographical annotation only.
