# ADR-046 — Canvas Viewport Persistence Across Back-Navigation

**Date:** 2026-08-05
**Status:** Accepted

---

## Context

When a user navigated away from a canvas screen (category canvas or person canvas)
and returned to it via the back stack, the pan and zoom position were reset to the
auto-fit defaults. This was surprising: users who had zoomed in on a region of
interest lost their context.

The root cause is that `rememberGraphViewportState()` used plain `remember {}`,
which survives recomposition but is discarded when the composable leaves the
composition (i.e., when another screen is pushed on the nav back stack).

Two approaches were considered:

1. **ViewModel stores zoom/pan in UiState** — works reliably but requires new
   state fields in every ViewModel that hosts a canvas, and forces the ViewModel
   layer to know about viewport concepts that are purely presentational.

2. **`rememberSaveable` with a custom `Saver`** — the viewport state is saved
   in the saved-state bundle tied to the composable's back-stack entry. No
   ViewModel changes required; the persistence boundary matches the navigation
   back-stack lifetime exactly.

Option 2 was chosen because it keeps viewport logic fully inside the `:canvas`
library with zero impact on any ViewModel.

---

## Decision

### 1. `GraphViewportState` gains initial-value constructor parameters

```kotlin
@Stable
class GraphViewportState(
    initialZoom: Float = 1f,
    initialPanOffset: Offset = Offset.Zero,
) {
    var zoom by mutableFloatStateOf(initialZoom)
    var panOffset by mutableStateOf(initialPanOffset)
}
```

### 2. `GraphViewportStateSaver` — `listSaver` serialising to three floats

```kotlin
private val GraphViewportStateSaver = listSaver<GraphViewportState, Float>(
    save = { listOf(it.zoom, it.panOffset.x, it.panOffset.y) },
    restore = { GraphViewportState(initialZoom = it[0], initialPanOffset = Offset(it[1], it[2])) },
)
```

### 3. `rememberGraphViewportState()` switches from `remember` to `rememberSaveable`

```kotlin
@Composable
fun rememberGraphViewportState(): GraphViewportState =
    rememberSaveable(saver = GraphViewportStateSaver) { GraphViewportState() }
```

The API surface is unchanged; existing call sites require no modification.

### 4. Auto-fit guard in `RelationGraph`

The `LaunchedEffect(nodes, canvasSize)` that computes the initial zoom/pan was
already present. It now skips its body when the viewport is not at its defaults,
so a restored (non-zero-pan or non-1-zoom) viewport is never overwritten:

```kotlin
LaunchedEffect(nodes, canvasSize) {
    if (nodes.isEmpty() || canvasSize == Size.Zero) return@LaunchedEffect
    if (viewportState.zoom != 1f || viewportState.panOffset != Offset.Zero) return@LaunchedEffect
    // ... auto-fit calculation ...
}
```

---

## Consequences

- Navigating back to any canvas screen restores the exact pan and zoom the user
  left it at, without any ViewModel changes.
- The auto-fit runs only on a fresh (default) viewport; it is suppressed when
  a saved viewport is restored.
- The `:canvas` library remains the single home for all viewport logic.
- Callers that pass an explicit `viewportState` (non-default, externally owned)
  are unaffected: `rememberGraphViewportState()` is still the default parameter
  value, and the guard only checks at the `LaunchedEffect` site.
