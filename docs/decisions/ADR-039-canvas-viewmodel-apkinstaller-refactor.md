# ADR-039: Shared CanvasViewModel Base Class and ApkInstaller Interface

**Date:** 2026-08-04
**Status:** Accepted

## Context

Two structural issues emerged from the growing codebase:

1. `CategoryCanvasViewModel` and `PersonCanvasViewModel` each contained
   identical implementations of `openRelationDialog`, `closeRelationDialog`,
   and `addRelationFromCanvas`, plus duplicated node/edge-building logic
   (`fromCounts`, `toCounts`, `categoryLists`). Any bug fix or new behaviour
   had to be applied in two places.

2. `UpdateViewModel` depended on `Context` and `OkHttpClient` directly,
   making it impossible to unit-test without Android instrumentation or a
   real HTTP client.

A third, smaller duplication existed in `CategoryCanvasViewModel` and
`AcquaintancesViewModel`: both maintained private `descendantsAndSelf`
methods (one recursive, one iterative) with different argument orderings.

## Decision

**Shared canvas infrastructure:**
- Add abstract `CanvasViewModel(relationRepository)` base class with the
  three shared dialog methods. Each subclass implements four abstract members
  (`dialogFromId`, `dialogToId`, `nodesSnapshot()`, `applyDialogState()`)
  to expose the relevant slice of its `UiState`.
- Extract `buildCanvasNodes(...)` and `buildCanvasEdges(...)` as
  `internal` top-level functions in `CanvasShared.kt`. Both ViewModels
  delegate to them.

**`descendantsAndSelf` extension:**
- Add `fun List<Category>.descendantsAndSelf(id: Long): Set<Long>` in
  `model/Category.kt` (iterative BFS). Both private copies removed.

**ApkInstaller interface:**
- Define `interface ApkInstaller { suspend fun prepareInstall(...): Intent }`
  in `data/ApkInstaller.kt`.
- Move the OkHttp download logic from `UpdateViewModel` into
  `OkHttpApkInstaller` (production impl, `@Inject` constructor with
  `@ApplicationContext Context` and `OkHttpClient`).
- `UpdateViewModel` now takes `UpdateRepository` and `ApkInstaller` —
  no Android types in its constructor, so it can be tested with plain JUnit.
- Add `FakeUpdateRepository`, `FakeApkInstaller`, and 11 unit tests in
  `UpdateViewModelTest`.

**`verticalDelta` on `RelationType`:**
- Add `val verticalDelta: Int = 0` to the `RelationType` data class;
  set the correct value for each hierarchical type. The BFS in
  `PersonCanvasViewModel` reads `RelationType.verticalDelta` rather than
  a raw-string `when` block.

## Alternatives Considered

| Option | Why rejected |
|--------|-------------|
| Keep duplication, fix each ViewModel independently | Accumulates drift; a bug fix in one must always be manually mirrored |
| Use a Kotlin delegation interface instead of abstract class | Delegation still requires boilerplate at each call site; the abstract class centralises the implementations entirely |
| Test UpdateViewModel with Robolectric | Adds a heavy test dependency; the interface extraction achieves the same result with zero new libraries |

## Consequences

- Dialog logic and node/edge builders are maintained in one place; both
  canvas ViewModels are noticeably shorter.
- `UpdateViewModel` is fully unit-testable without Android instrumentation.
- `verticalDelta` is co-located with its type definitions, making it
  impossible to add a new hierarchical relation type without also specifying
  its layout delta.
- `descendantsAndSelf` is now in the model layer where it belongs; callers
  read as `categories.descendantsAndSelf(id)` — receiver syntax matches
  idiomatic Kotlin.
