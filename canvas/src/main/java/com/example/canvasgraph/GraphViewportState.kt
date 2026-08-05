package com.example.canvasgraph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

@Stable
class GraphViewportState {
    var zoom by mutableFloatStateOf(1f)
    var panOffset by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberGraphViewportState(): GraphViewportState = remember { GraphViewportState() }
