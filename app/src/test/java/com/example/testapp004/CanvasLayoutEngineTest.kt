package com.example.testapp004

import com.example.canvasgraph.ForceDirectedLayoutEngine
import com.example.canvasgraph.HierarchicalLayoutEngine
import com.example.canvasgraph.LayoutEdge
import com.example.canvasgraph.RadialLayoutEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class CanvasLayoutEngineTest {
    // --- RadialLayoutEngine ---

    @Test
    fun `radial layout assigns a position to every node`() {
        val engine = RadialLayoutEngine()
        val nodeIds = setOf(1L, 2L, 3L)
        val positions = engine.computePositions(nodeIds, emptyList())
        assertEquals(3, positions.size)
        assertTrue(nodeIds.all { it in positions })
    }

    @Test
    fun `radial layout handles a single isolated node`() {
        val engine = RadialLayoutEngine()
        val positions = engine.computePositions(setOf(42L), emptyList())
        assertEquals(1, positions.size)
        assertTrue(42L in positions)
    }

    @Test
    fun `radial layout handles empty graph`() {
        val engine = RadialLayoutEngine()
        val positions = engine.computePositions(emptySet(), emptyList())
        assertTrue(positions.isEmpty())
    }

    @Test
    fun `radial layout assigns positions to connected nodes`() {
        val engine = RadialLayoutEngine()
        val edge = LayoutEdge(fromId = 1L, toId = 2L)
        val positions = engine.computePositions(setOf(1L, 2L), listOf(edge))
        assertEquals(2, positions.size)
    }

    @Test
    fun `radial layout places isolated nodes separately from connected cluster`() {
        val engine = RadialLayoutEngine()
        val edge = LayoutEdge(fromId = 1L, toId = 2L)
        val positions = engine.computePositions(setOf(1L, 2L, 99L), listOf(edge))
        assertEquals(3, positions.size)
        val pos99 = positions[99L]!!
        assertTrue(positions.entries.none { (id, pos) -> id != 99L && pos == pos99 })
    }

    @Test
    fun `radial layout ignores edges referencing nodes outside the visible set`() {
        val engine = RadialLayoutEngine()
        val edge = LayoutEdge(fromId = 1L, toId = 999L)
        val positions = engine.computePositions(setOf(1L, 2L), listOf(edge))
        assertEquals(2, positions.size)
    }

    // --- ForceDirectedLayoutEngine ---

    @Test
    fun `force-directed layout assigns a position to every node`() {
        val engine = ForceDirectedLayoutEngine()
        val nodeIds = setOf(1L, 2L, 3L)
        val positions = engine.computePositions(nodeIds, emptyList())
        assertEquals(3, positions.size)
        assertTrue(nodeIds.all { it in positions })
    }

    @Test
    fun `force-directed layout handles a single isolated node`() {
        val engine = ForceDirectedLayoutEngine()
        val positions = engine.computePositions(setOf(42L), emptyList())
        assertEquals(1, positions.size)
        assertTrue(42L in positions)
    }

    @Test
    fun `force-directed layout handles empty graph`() {
        val engine = ForceDirectedLayoutEngine()
        val positions = engine.computePositions(emptySet(), emptyList())
        assertTrue(positions.isEmpty())
    }

    @Test
    fun `force-directed layout assigns positions to connected nodes`() {
        val engine = ForceDirectedLayoutEngine()
        val edge = LayoutEdge(fromId = 1L, toId = 2L)
        val positions = engine.computePositions(setOf(1L, 2L), listOf(edge))
        assertEquals(2, positions.size)
    }

    @Test
    fun `force-directed layout places isolated nodes separately from connected cluster`() {
        val engine = ForceDirectedLayoutEngine()
        val edge = LayoutEdge(fromId = 1L, toId = 2L)
        val positions = engine.computePositions(setOf(1L, 2L, 99L), listOf(edge))
        assertEquals(3, positions.size)
        val pos99 = positions[99L]!!
        assertTrue(positions.entries.none { (id, pos) -> id != 99L && pos == pos99 })
    }

    @Test
    fun `force-directed layout ignores edges referencing nodes outside the visible set`() {
        val engine = ForceDirectedLayoutEngine()
        val edge = LayoutEdge(fromId = 1L, toId = 999L)
        val positions = engine.computePositions(setOf(1L, 2L), listOf(edge))
        assertEquals(2, positions.size)
    }

    @Test
    fun `force-directed layout places connected nodes closer than isolated nodes`() {
        val engine = ForceDirectedLayoutEngine()
        val edge = LayoutEdge(fromId = 1L, toId = 2L)
        val positions = engine.computePositions(setOf(1L, 2L, 3L), listOf(edge))
        val (x1, y1) = positions[1L]!!
        val (x2, y2) = positions[2L]!!
        val (x3, y3) = positions[3L]!!
        val d12 = sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
        val d13 = sqrt((x3 - x1) * (x3 - x1) + (y3 - y1) * (y3 - y1))
        assertTrue("Connected pair (1–2) should be closer than isolated node (3)", d12 < d13)
    }

    @Test
    fun `force-directed layout is deterministic`() {
        val engine = ForceDirectedLayoutEngine()
        val edges = listOf(
            LayoutEdge(fromId = 1L, toId = 2L),
            LayoutEdge(fromId = 2L, toId = 3L),
        )
        val nodes = setOf(1L, 2L, 3L, 4L)
        val pos1 = engine.computePositions(nodes, edges)
        val pos2 = engine.computePositions(nodes, edges)
        nodes.forEach { id ->
            assertEquals(pos1[id]!!.first, pos2[id]!!.first, 0.001f)
            assertEquals(pos1[id]!!.second, pos2[id]!!.second, 0.001f)
        }
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
        val edge = LayoutEdge(fromId = 2L, toId = 1L) // symmetric edge, weight 0
        val positions = engine.computePositions(setOf(1L, 2L), listOf(edge), rootId = 1L)
        val root = positions[1L]!!
        assertEquals(0f, root.first, 0.001f)
        assertEquals(0f, root.second, 0.001f)
    }

    @Test
    fun `hierarchical layout places parent above center (negative y)`() {
        val engine = HierarchicalLayoutEngine()
        // Bob (fromId=2) is parent of Alice (toId=1, the root). verticalWeight=1 (PARENT_CHILD).
        val edge = LayoutEdge(fromId = 2L, toId = 1L, verticalWeight = 1)
        val positions = engine.computePositions(setOf(1L, 2L), listOf(edge), rootId = 1L)
        val parentY = positions[2L]!!.second
        assertTrue("Parent should be above center (y < 0)", parentY < 0f)
    }

    @Test
    fun `hierarchical layout places child below center (positive y)`() {
        val engine = HierarchicalLayoutEngine()
        // Alice (fromId=1, the root) is parent of Bob (toId=2). verticalWeight=1 (PARENT_CHILD).
        val edge = LayoutEdge(fromId = 1L, toId = 2L, verticalWeight = 1)
        val positions = engine.computePositions(setOf(1L, 2L), listOf(edge), rootId = 1L)
        val childY = positions[2L]!!.second
        assertTrue("Child should be below center (y > 0)", childY > 0f)
    }

    @Test
    fun `hierarchical layout places sibling at same y as center`() {
        val engine = HierarchicalLayoutEngine()
        val edge = LayoutEdge(fromId = 1L, toId = 2L, verticalWeight = 0) // SIBLING weight=0
        val positions = engine.computePositions(setOf(1L, 2L), listOf(edge), rootId = 1L)
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
        val edge = LayoutEdge(fromId = 1L, toId = 2L)
        val positions = engine.computePositions(setOf(1L, 2L, 3L), listOf(edge), rootId = 1L)
        val isolatedY = positions[3L]!!.second
        assertEquals(0f, isolatedY, 0.001f)
    }

    @Test
    fun `hierarchical layout places spouse-with-no-parents left of sibling sharing parents`() {
        val engine = HierarchicalLayoutEngine()
        // Esti(1) is the root. Fernando(2) is Esti's spouse — no connections to the
        // parent layer (Juanje=6, Maite=7). Inigo(3) is Esti's sibling, also a child
        // of Juanje and Maite. Both Esti and Fernando are parents of Jon(4) and Irati(5).
        val relations = listOf(
            LayoutEdge(fromId = 1L, toId = 2L, verticalWeight = 0),
            LayoutEdge(fromId = 1L, toId = 3L, verticalWeight = 0),
            LayoutEdge(fromId = 6L, toId = 1L, verticalWeight = 1),
            LayoutEdge(fromId = 6L, toId = 3L, verticalWeight = 1),
            LayoutEdge(fromId = 7L, toId = 1L, verticalWeight = 1),
            LayoutEdge(fromId = 7L, toId = 3L, verticalWeight = 1),
            LayoutEdge(fromId = 1L, toId = 4L, verticalWeight = 1),
            LayoutEdge(fromId = 1L, toId = 5L, verticalWeight = 1),
            LayoutEdge(fromId = 2L, toId = 4L, verticalWeight = 1),
            LayoutEdge(fromId = 2L, toId = 5L, verticalWeight = 1),
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
        // Both parents (Juanje=6, Maite=7) are parents of all three siblings.
        // Fernando additionally has children Jon(4) and Irati(5).
        val relations = listOf(
            LayoutEdge(fromId = 1L, toId = 2L, verticalWeight = 0),
            LayoutEdge(fromId = 1L, toId = 3L, verticalWeight = 0),
            LayoutEdge(fromId = 6L, toId = 1L, verticalWeight = 1),
            LayoutEdge(fromId = 6L, toId = 2L, verticalWeight = 1),
            LayoutEdge(fromId = 6L, toId = 3L, verticalWeight = 1),
            LayoutEdge(fromId = 7L, toId = 1L, verticalWeight = 1),
            LayoutEdge(fromId = 7L, toId = 2L, verticalWeight = 1),
            LayoutEdge(fromId = 7L, toId = 3L, verticalWeight = 1),
            LayoutEdge(fromId = 2L, toId = 4L, verticalWeight = 1),
            LayoutEdge(fromId = 2L, toId = 5L, verticalWeight = 1),
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
        // Root(1), SibL(2), SibR(3) at level 0; PL(10) and PR(11) at level 1.
        // PL is parent of Root and SibL; PR is parent of Root and SibR.
        val relations = listOf(
            LayoutEdge(fromId = 1L, toId = 2L, verticalWeight = 0),
            LayoutEdge(fromId = 1L, toId = 3L, verticalWeight = 0),
            LayoutEdge(fromId = 10L, toId = 1L, verticalWeight = 1),
            LayoutEdge(fromId = 10L, toId = 2L, verticalWeight = 1),
            LayoutEdge(fromId = 11L, toId = 1L, verticalWeight = 1),
            LayoutEdge(fromId = 11L, toId = 3L, verticalWeight = 1),
        )
        val positions = engine.computePositions(
            nodeIds = setOf(1L, 2L, 3L, 10L, 11L),
            edges = relations,
            rootId = 1L,
        )
        val sibLX = positions[2L]!!.first
        val sibRX = positions[3L]!!.first
        assertTrue(
            "SibL (child of left parent PL) should be left of SibR (child of right parent PR)",
            sibLX < sibRX,
        )
    }

    @Test
    fun `hierarchical layout is deterministic regardless of input set order`() {
        val engine = HierarchicalLayoutEngine()
        val relations = listOf(
            LayoutEdge(fromId = 6L, toId = 1L, verticalWeight = 1),
            LayoutEdge(fromId = 6L, toId = 2L, verticalWeight = 1),
            LayoutEdge(fromId = 6L, toId = 3L, verticalWeight = 1),
        )
        val nodes = setOf(1L, 2L, 3L, 6L)
        val pos1 = engine.computePositions(nodes, relations, rootId = 1L)
        val pos2 = engine.computePositions(nodes, relations, rootId = 1L)
        assertEquals(pos1[2L]!!.first, pos2[2L]!!.first, 0.001f)
        assertEquals(pos1[3L]!!.first, pos2[3L]!!.first, 0.001f)
    }
}
