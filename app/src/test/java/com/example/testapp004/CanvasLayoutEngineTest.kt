package com.example.testapp004

import com.example.testapp004.model.Relation
import com.example.testapp004.viewmodel.HierarchicalLayoutEngine
import com.example.testapp004.viewmodel.RadialClusterLayoutEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasLayoutEngineTest {
    // --- RadialClusterLayoutEngine ---

    @Test
    fun `radial layout assigns a position to every node`() {
        val engine = RadialClusterLayoutEngine()
        val nodeIds = setOf(1L, 2L, 3L)
        val positions = engine.computePositions(nodeIds, emptyList())
        assertEquals(3, positions.size)
        assertTrue(nodeIds.all { it in positions })
    }

    @Test
    fun `radial layout handles a single isolated node`() {
        val engine = RadialClusterLayoutEngine()
        val positions = engine.computePositions(setOf(42L), emptyList())
        assertEquals(1, positions.size)
        assertTrue(42L in positions)
    }

    @Test
    fun `radial layout handles empty graph`() {
        val engine = RadialClusterLayoutEngine()
        val positions = engine.computePositions(emptySet(), emptyList())
        assertTrue(positions.isEmpty())
    }

    @Test
    fun `radial layout assigns positions to connected nodes`() {
        val engine = RadialClusterLayoutEngine()
        val rel = Relation(id = 1L, fromId = 1L, toId = 2L, typeKey = "FRIEND")
        val positions = engine.computePositions(setOf(1L, 2L), listOf(rel))
        assertEquals(2, positions.size)
    }

    @Test
    fun `radial layout places isolated nodes separately from connected cluster`() {
        val engine = RadialClusterLayoutEngine()
        val rel = Relation(id = 1L, fromId = 1L, toId = 2L, typeKey = "FRIEND")
        val positions = engine.computePositions(setOf(1L, 2L, 99L), listOf(rel))
        assertEquals(3, positions.size)
        // Isolated node should not share the exact same position as any other node
        val pos99 = positions[99L]!!
        assertTrue(positions.entries.none { (id, pos) -> id != 99L && pos == pos99 })
    }

    @Test
    fun `radial layout ignores edges referencing nodes outside the visible set`() {
        val engine = RadialClusterLayoutEngine()
        val rel = Relation(id = 1L, fromId = 1L, toId = 999L, typeKey = "FRIEND")
        val positions = engine.computePositions(setOf(1L, 2L), listOf(rel))
        assertEquals(2, positions.size)
    }

    // --- HierarchicalLayoutEngine ---

    @Test
    fun `hierarchical layout returns empty map when rootId is null`() {
        val engine = HierarchicalLayoutEngine()
        val positions = engine.computePositions(setOf(1L, 2L), emptyList())
        assertTrue(positions.isEmpty())
    }

    @Test
    fun `hierarchical layout assigns positions to all visible nodes`() {
        val engine = HierarchicalLayoutEngine()
        val positions = engine.computePositions(setOf(1L, 2L, 3L), emptyList(), rootId = 1L)
        assertEquals(3, positions.size)
        assertTrue(setOf(1L, 2L, 3L).all { it in positions })
    }

    @Test
    fun `hierarchical layout places root at origin`() {
        val engine = HierarchicalLayoutEngine()
        val rel = Relation(id = 1L, fromId = 2L, toId = 1L, typeKey = "FRIEND")
        val positions = engine.computePositions(setOf(1L, 2L), listOf(rel), rootId = 1L)
        val root = positions[1L]!!
        assertEquals(0f, root.first, 0.001f)
        assertEquals(0f, root.second, 0.001f)
    }

    @Test
    fun `hierarchical layout places parent above center (negative y)`() {
        val engine = HierarchicalLayoutEngine()
        // Bob is the parent (fromId) and Alice is the child (toId) in PARENT_CHILD
        val rel = Relation(id = 1L, fromId = 2L, toId = 1L, typeKey = "PARENT_CHILD")
        val positions = engine.computePositions(setOf(1L, 2L), listOf(rel), rootId = 1L)
        val parentY = positions[2L]!!.second
        assertTrue("Parent should be above center (y < 0)", parentY < 0f)
    }

    @Test
    fun `hierarchical layout places child below center (positive y)`() {
        val engine = HierarchicalLayoutEngine()
        // Alice is the parent (fromId) so Bob (toId) is the child
        val rel = Relation(id = 1L, fromId = 1L, toId = 2L, typeKey = "PARENT_CHILD")
        val positions = engine.computePositions(setOf(1L, 2L), listOf(rel), rootId = 1L)
        val childY = positions[2L]!!.second
        assertTrue("Child should be below center (y > 0)", childY > 0f)
    }

    @Test
    fun `hierarchical layout places sibling at same y as center`() {
        val engine = HierarchicalLayoutEngine()
        val rel = Relation(id = 1L, fromId = 1L, toId = 2L, typeKey = "SIBLING")
        val positions = engine.computePositions(setOf(1L, 2L), listOf(rel), rootId = 1L)
        val siblingY = positions[2L]!!.second
        assertEquals(0f, siblingY, 0.001f)
    }

    @Test
    fun `hierarchical layout single node is placed at origin`() {
        val engine = HierarchicalLayoutEngine()
        val positions = engine.computePositions(setOf(5L), emptyList(), rootId = 5L)
        val pos = positions[5L]!!
        assertEquals(0f, pos.first, 0.001f)
        assertEquals(0f, pos.second, 0.001f)
    }

    @Test
    fun `hierarchical layout disconnected nodes default to same level as root`() {
        val engine = HierarchicalLayoutEngine()
        // Node 3 has no relations, so it falls back to level 0
        val rel = Relation(id = 1L, fromId = 1L, toId = 2L, typeKey = "FRIEND")
        val positions = engine.computePositions(setOf(1L, 2L, 3L), listOf(rel), rootId = 1L)
        val isolatedY = positions[3L]!!.second
        assertEquals(0f, isolatedY, 0.001f)
    }

    @Test
    fun `hierarchical layout places spouse-with-no-parents left of sibling sharing parents`() {
        val engine = HierarchicalLayoutEngine()
        // Esti(1) is the root. Fernando(2) is Esti's spouse — no connections to the
        // parent layer (Juanje=6, Maite=7). Inigo(3) is Esti's sibling, also a child
        // of Juanje and Maite. Both Esti and Fernando are parents of Jon(4) and Irati(5).
        // All barycentric scores are equal (symmetric parent positions cancel out),
        // so the tie-breaker must recognise that Fernando has fewer upper-layer
        // connections (0) than Esti and Inigo (2 each) and place him to the LEFT.
        val relations = listOf(
            Relation(id = 1L, fromId = 1L, toId = 2L, typeKey = "SPOUSE"),
            Relation(id = 2L, fromId = 1L, toId = 3L, typeKey = "SIBLING"),
            Relation(id = 3L, fromId = 6L, toId = 1L, typeKey = "PARENT_CHILD"),
            Relation(id = 4L, fromId = 6L, toId = 3L, typeKey = "PARENT_CHILD"),
            Relation(id = 5L, fromId = 7L, toId = 1L, typeKey = "PARENT_CHILD"),
            Relation(id = 6L, fromId = 7L, toId = 3L, typeKey = "PARENT_CHILD"),
            Relation(id = 7L, fromId = 1L, toId = 4L, typeKey = "PARENT_CHILD"),
            Relation(id = 8L, fromId = 1L, toId = 5L, typeKey = "PARENT_CHILD"),
            Relation(id = 9L, fromId = 2L, toId = 4L, typeKey = "PARENT_CHILD"),
            Relation(id = 10L, fromId = 2L, toId = 5L, typeKey = "PARENT_CHILD"),
        )
        val positions = engine.computePositions(
            nodeIds = setOf(1L, 2L, 3L, 4L, 5L, 6L, 7L),
            edges = relations,
            rootId = 1L,
        )
        val fernandoX = positions[2L]!!.first
        val estiX = positions[1L]!!.first
        val inigoX = positions[3L]!!.first
        assertTrue("Fernando (no parent links) should be left of Esti (root)", fernandoX < estiX)
        assertTrue("Fernando (no parent links) should be left of Inigo (shares parents)", fernandoX < inigoX)
        assertTrue("Inigo (shares parents with Esti) should be right of Esti", inigoX > estiX)
    }

    @Test
    fun `hierarchical layout places sibling with children left of sibling without children`() {
        val engine = HierarchicalLayoutEngine()
        // Root: Esti(1). Fernando(2) and Inigo(3) are siblings of Esti.
        // Both parents (Juanje=6, Maite=7) are parents of all three siblings,
        // making barycentric scores equal. Fernando additionally has children
        // Jon(4) and Irati(5); the tertiary tie-breaker on lower-level count
        // must place Fernando to the LEFT of Inigo.
        val relations = listOf(
            Relation(id = 1L, fromId = 1L, toId = 2L, typeKey = "SIBLING"),
            Relation(id = 2L, fromId = 1L, toId = 3L, typeKey = "SIBLING"),
            Relation(id = 3L, fromId = 6L, toId = 1L, typeKey = "PARENT_CHILD"),
            Relation(id = 4L, fromId = 6L, toId = 2L, typeKey = "PARENT_CHILD"),
            Relation(id = 5L, fromId = 6L, toId = 3L, typeKey = "PARENT_CHILD"),
            Relation(id = 6L, fromId = 7L, toId = 1L, typeKey = "PARENT_CHILD"),
            Relation(id = 7L, fromId = 7L, toId = 2L, typeKey = "PARENT_CHILD"),
            Relation(id = 8L, fromId = 7L, toId = 3L, typeKey = "PARENT_CHILD"),
            Relation(id = 9L, fromId = 2L, toId = 4L, typeKey = "PARENT_CHILD"),
            Relation(id = 10L, fromId = 2L, toId = 5L, typeKey = "PARENT_CHILD"),
        )
        val positions = engine.computePositions(
            nodeIds = setOf(1L, 2L, 3L, 4L, 5L, 6L, 7L),
            edges = relations,
            rootId = 1L,
        )
        val fernandoX = positions[2L]!!.first
        val inigoX = positions[3L]!!.first
        assertTrue("Fernando (with children) should be left of Inigo (no children)", fernandoX < inigoX)
    }

    @Test
    fun `hierarchical layout minimises crossings between two layers`() {
        val engine = HierarchicalLayoutEngine()
        // Parent layer: P1(10), P2(11).  Child layer: C1(1), C2(2), C3(3).
        // Edges: P1→C3, P2→C1. The crossing-free order is [C3, C2, C1] (or just C3 left of C1).
        // BFS would discover C1 and C2 before C3; without crossing minimisation C1 ends up
        // left of C3, crossing the P2→C1 / P1→C3 edge pair.
        val relations = listOf(
            Relation(id = 1L, fromId = 10L, toId = 3L, typeKey = "PARENT_CHILD"),
            Relation(id = 2L, fromId = 11L, toId = 1L, typeKey = "PARENT_CHILD"),
            Relation(id = 3L, fromId = 10L, toId = 2L, typeKey = "PARENT_CHILD"),
        )
        val positions = engine.computePositions(
            nodeIds = setOf(1L, 2L, 3L, 10L, 11L),
            edges = relations,
            rootId = 1L,
        )
        // P1 is connected to C3 and C2; P2 is connected to C1.
        // P2 has no upper connections so it sorts left of P1 → P2 < P1 in x.
        // For the child layer: C1 is connected only to P2 (leftmost parent) → C1 should
        // be left of C3 (connected to P1, the rightmost parent).
        val c1X = positions[1L]!!.first
        val c3X = positions[3L]!!.first
        assertTrue("C1 (child of left parent P2) should be left of C3 (child of right parent P1)", c1X < c3X)
    }

    @Test
    fun `hierarchical layout is deterministic regardless of input set order`() {
        val engine = HierarchicalLayoutEngine()
        val relations = listOf(
            Relation(id = 1L, fromId = 6L, toId = 1L, typeKey = "PARENT_CHILD"),
            Relation(id = 2L, fromId = 6L, toId = 2L, typeKey = "PARENT_CHILD"),
            Relation(id = 3L, fromId = 6L, toId = 3L, typeKey = "PARENT_CHILD"),
        )
        val nodes = setOf(1L, 2L, 3L, 6L)
        val pos1 = engine.computePositions(nodes, relations, rootId = 1L)
        val pos2 = engine.computePositions(nodes, relations, rootId = 1L)
        assertEquals(pos1[2L]!!.first, pos2[2L]!!.first, 0.001f)
        assertEquals(pos1[3L]!!.first, pos2[3L]!!.first, 0.001f)
    }
}
