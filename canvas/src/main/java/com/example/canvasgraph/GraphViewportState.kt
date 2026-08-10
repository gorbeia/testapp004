package com.example.canvasgraph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

@Stable
class GraphViewportState(
    initialZoom: Float = 1f,
    initialPanOffset: Offset = Offset.Zero,
) {
    var zoom by mutableFloatStateOf(initialZoom)
    var panOffset by mutableStateOf(initialPanOffset)

    fun reset() {
        zoom = 1f
        panOffset = Offset.Zero
    }
}

private val GraphViewportStateSaver = listSaver<GraphViewportState, Float>(
    save = { listOf(it.zoom, it.panOffset.x, it.panOffset.y) },
    restore = { GraphViewportState(initialZoom = it[0], initialPanOffset = Offset(it[1], it[2])) },
)

@Composable
fun rememberGraphViewportState(): GraphViewportState =
    rememberSaveable(saver = GraphViewportStateSaver) { GraphViewportState() }
