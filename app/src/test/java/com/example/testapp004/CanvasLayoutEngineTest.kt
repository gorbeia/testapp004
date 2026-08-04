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
}
