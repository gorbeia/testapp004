package com.example.canvasgraph

enum class LayoutEngineType(val displayName: String) {
    Hierarchical("Hierarchical"),
    ForceDirected("Force-Directed"),
    Radial("Radial"),
}

fun LayoutEngineType.createEngine(): GraphLayoutEngine = when (this) {
    LayoutEngineType.Hierarchical -> HierarchicalLayoutEngine()
    LayoutEngineType.ForceDirected -> ForceDirectedLayoutEngine()
    LayoutEngineType.Radial -> RadialLayoutEngine()
}
