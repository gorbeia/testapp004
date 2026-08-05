package com.example.canvasgraph

enum class LayoutEngineType(val displayName: String) {
    Hierarchical("Hierarchical"),
    ForceDirected("Force-Directed"),
    Radial("Radial"),
    Circular("Circular"),
    Arc("Arc"),
    Cluster("Cluster"),
}

fun LayoutEngineType.createEngine(): GraphLayoutEngine = when (this) {
    LayoutEngineType.Hierarchical -> HierarchicalLayoutEngine()
    LayoutEngineType.ForceDirected -> ForceDirectedLayoutEngine()
    LayoutEngineType.Radial -> RadialLayoutEngine()
    LayoutEngineType.Circular -> CircularLayoutEngine()
    LayoutEngineType.Arc -> ArcLayoutEngine()
    LayoutEngineType.Cluster -> ClusterLayoutEngine()
}
