package com.example.testapp004

import com.example.testapp004.util.MainDispatcherRule
import com.example.testapp004.viewmodel.CategoriesViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CategoriesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeCategoryRepository: FakeCategoryRepository
    private lateinit var viewModel: CategoriesViewModel

    @Before
    fun setup() {
        fakeCategoryRepository = FakeCategoryRepository()
        viewModel = CategoriesViewModel(fakeCategoryRepository)
    }

    @Test
    fun `initial state has empty categories list`() {
        assertTrue(viewModel.uiState.value.categories.isEmpty())
    }

    @Test
    fun `initial state has dialog closed`() {
        assertFalse(viewModel.uiState.value.isAddDialogOpen)
    }

    @Test
    fun `openAddDialog sets flag to true`() {
        viewModel.openAddDialog()
        assertTrue(viewModel.uiState.value.isAddDialogOpen)
    }

    @Test
    fun `closeAddDialog sets flag to false`() {
        viewModel.openAddDialog()
        viewModel.closeAddDialog()
        assertFalse(viewModel.uiState.value.isAddDialogOpen)
    }

    @Test
    fun `addCategory creates new category and closes dialog`() {
        viewModel.openAddDialog()
        viewModel.addCategory("Work")
        assertEquals(1, viewModel.uiState.value.categories.size)
        assertEquals("Work", viewModel.uiState.value.categories.first().name)
        assertFalse(viewModel.uiState.value.isAddDialogOpen)
    }

    @Test
    fun `addCategory with blank name does nothing`() {
        viewModel.addCategory("   ")
        assertTrue(viewModel.uiState.value.categories.isEmpty())
    }

    @Test
    fun `addCategory trims whitespace`() {
        viewModel.addCategory("  Family  ")
        assertEquals("Family", viewModel.uiState.value.categories.first().name)
    }

    @Test
    fun `addCategory with parent creates child category`() {
        viewModel.addCategory("Work")
        val parentId = viewModel.uiState.value.categories.first().id
        viewModel.addCategory("Engineering", parentId)
        val child = viewModel.uiState.value.categories.find { it.name == "Engineering" }
        assertNotNull(child)
        assertEquals(parentId, child?.parentId)
    }

    @Test
    fun `addCategory without parent creates root category`() {
        viewModel.addCategory("Family")
        assertNull(viewModel.uiState.value.categories.first().parentId)
    }

    @Test
    fun `deleteCategory removes it from list`() {
        viewModel.addCategory("Work")
        val id = viewModel.uiState.value.categories.first().id
        viewModel.deleteCategory(id)
        assertTrue(viewModel.uiState.value.categories.isEmpty())
    }

    @Test
    fun `deleteCategory only removes the targeted category`() {
        viewModel.addCategory("Work")
        viewModel.addCategory("Family")
        val workId = viewModel.uiState.value.categories.first().id
        viewModel.deleteCategory(workId)
        assertEquals(1, viewModel.uiState.value.categories.size)
        assertEquals("Family", viewModel.uiState.value.categories.first().name)
    }

    @Test
    fun `deleteCategory orphans its children`() {
        viewModel.addCategory("Work")
        val parentId = viewModel.uiState.value.categories.first().id
        viewModel.addCategory("Engineering", parentId)
        viewModel.deleteCategory(parentId)
        val child = viewModel.uiState.value.categories.find { it.name == "Engineering" }
        assertNotNull(child)
        assertNull(child?.parentId)
    }

    @Test
    fun `categories assigned unique ids`() {
        viewModel.addCategory("Work")
        viewModel.addCategory("Family")
        val ids = viewModel.uiState.value.categories.map { it.id }
        assertEquals(ids.distinct(), ids)
    }

    @Test
    fun `openEditDialog sets editingCategory`() {
        viewModel.addCategory("Work")
        val category = viewModel.uiState.value.categories.first()
        viewModel.openEditDialog(category)
        assertEquals(category, viewModel.uiState.value.editingCategory)
    }

    @Test
    fun `closeEditDialog clears editingCategory`() {
        viewModel.addCategory("Work")
        val category = viewModel.uiState.value.categories.first()
        viewModel.openEditDialog(category)
        viewModel.closeEditDialog()
        assertNull(viewModel.uiState.value.editingCategory)
    }

    @Test
    fun `updateCategory renames the category and closes dialog`() {
        viewModel.addCategory("Work")
        val id = viewModel.uiState.value.categories.first().id
        viewModel.openEditDialog(viewModel.uiState.value.categories.first())
        viewModel.updateCategory(id, "Work & Business", null)
        val updated = viewModel.uiState.value.categories.find { it.id == id }
        assertEquals("Work & Business", updated?.name)
        assertNull(viewModel.uiState.value.editingCategory)
    }

    @Test
    fun `updateCategory trims whitespace`() {
        viewModel.addCategory("Work")
        val id = viewModel.uiState.value.categories.first().id
        viewModel.updateCategory(id, "  Family  ", null)
        assertEquals("Family", viewModel.uiState.value.categories.find { it.id == id }?.name)
    }

    @Test
    fun `updateCategory with blank name does nothing`() {
        viewModel.addCategory("Work")
        val id = viewModel.uiState.value.categories.first().id
        viewModel.openEditDialog(viewModel.uiState.value.categories.first())
        viewModel.updateCategory(id, "   ", null)
        assertEquals("Work", viewModel.uiState.value.categories.find { it.id == id }?.name)
        assertNotNull(viewModel.uiState.value.editingCategory)
    }

    @Test
    fun `updateCategory can change parent`() {
        viewModel.addCategory("Work")
        viewModel.addCategory("Family")
        val workId = viewModel.uiState.value.categories.first { it.name == "Work" }.id
        val familyId = viewModel.uiState.value.categories.first { it.name == "Family" }.id
        viewModel.updateCategory(workId, "Work", familyId)
        assertEquals(familyId, viewModel.uiState.value.categories.find { it.id == workId }?.parentId)
    }

    @Test
    fun `updateCategory can promote to root`() {
        viewModel.addCategory("Work")
        val parentId = viewModel.uiState.value.categories.first().id
        viewModel.addCategory("Engineering", parentId)
        val childId = viewModel.uiState.value.categories.first { it.name == "Engineering" }.id
        viewModel.updateCategory(childId, "Engineering", null)
        assertNull(viewModel.uiState.value.categories.find { it.id == childId }?.parentId)
    }

    // --- add child dialog ---

    @Test
    fun `openAddChildDialog sets addChildToCategory and closes regular dialog`() {
        viewModel.addCategory("Work")
        val category = viewModel.uiState.value.categories.first()
        viewModel.openAddDialog()
        viewModel.openAddChildDialog(category)
        assertEquals(category, viewModel.uiState.value.addChildToCategory)
        assertFalse(viewModel.uiState.value.isAddDialogOpen)
    }

    @Test
    fun `closeAddChildDialog clears addChildToCategory`() {
        viewModel.addCategory("Work")
        val category = viewModel.uiState.value.categories.first()
        viewModel.openAddChildDialog(category)
        viewModel.closeAddChildDialog()
        assertNull(viewModel.uiState.value.addChildToCategory)
    }

    @Test
    fun `openAddDialog clears addChildToCategory`() {
        viewModel.addCategory("Work")
        viewModel.openAddChildDialog(viewModel.uiState.value.categories.first())
        viewModel.openAddDialog()
        assertNull(viewModel.uiState.value.addChildToCategory)
        assertTrue(viewModel.uiState.value.isAddDialogOpen)
    }

    @Test
    fun `addCategory closes addChildToCategory dialog`() {
        viewModel.addCategory("Work")
        val parentId = viewModel.uiState.value.categories.first().id
        viewModel.openAddChildDialog(viewModel.uiState.value.categories.first())
        viewModel.addCategory("Engineering", parentId)
        assertNull(viewModel.uiState.value.addChildToCategory)
    }

    // --- sort order ---

    @Test
    fun `addCategory assigns increasing sort order within siblings`() {
        viewModel.addCategory("Alpha")
        viewModel.addCategory("Beta")
        viewModel.addCategory("Gamma")
        val orders = viewModel.uiState.value.categories
            .sortedBy { it.sortOrder }
            .map { it.name }
        assertEquals(listOf("Alpha", "Beta", "Gamma"), orders)
    }

    @Test
    fun `addCategory assigns sort order per parent group independently`() {
        viewModel.addCategory("Root1")
        viewModel.addCategory("Root2")
        val root1Id = viewModel.uiState.value.categories.first { it.name == "Root1" }.id
        val root2Id = viewModel.uiState.value.categories.first { it.name == "Root2" }.id
        viewModel.addCategory("Child1A", root1Id)
        viewModel.addCategory("Child2A", root2Id)
        viewModel.addCategory("Child1B", root1Id)
        val child1A = viewModel.uiState.value.categories.first { it.name == "Child1A" }
        val child1B = viewModel.uiState.value.categories.first { it.name == "Child1B" }
        val child2A = viewModel.uiState.value.categories.first { it.name == "Child2A" }
        assertTrue("Child1A should be before Child1B", child1A.sortOrder < child1B.sortOrder)
        assertTrue("Child2A should have sort order 0", child2A.sortOrder == 0)
    }

    // --- reorder ---

    @Test
    fun `reorderCategory moves item to target position`() {
        viewModel.addCategory("A")
        viewModel.addCategory("B")
        viewModel.addCategory("C")
        val aId = viewModel.uiState.value.categories.first { it.name == "A" }.id
        val cId = viewModel.uiState.value.categories.first { it.name == "C" }.id
        viewModel.reorderCategory(aId, cId)
        val ordered = viewModel.uiState.value.categories.sortedWith(
            compareBy({ it.sortOrder }, { it.id }),
        ).map { it.name }
        assertEquals(listOf("B", "C", "A"), ordered)
    }

    @Test
    fun `reorderCategory moving item up works correctly`() {
        viewModel.addCategory("A")
        viewModel.addCategory("B")
        viewModel.addCategory("C")
        val cId = viewModel.uiState.value.categories.first { it.name == "C" }.id
        val aId = viewModel.uiState.value.categories.first { it.name == "A" }.id
        viewModel.reorderCategory(cId, aId)
        val ordered = viewModel.uiState.value.categories.sortedWith(
            compareBy({ it.sortOrder }, { it.id }),
        ).map { it.name }
        assertEquals(listOf("C", "A", "B"), ordered)
    }

    @Test
    fun `reorderCategory does nothing for different parents`() {
        viewModel.addCategory("Root1")
        viewModel.addCategory("Root2")
        val root1Id = viewModel.uiState.value.categories.first { it.name == "Root1" }.id
        val root2Id = viewModel.uiState.value.categories.first { it.name == "Root2" }.id
        viewModel.addCategory("Child", root1Id)
        val childId = viewModel.uiState.value.categories.first { it.name == "Child" }.id
        val before = viewModel.uiState.value.categories.find { it.id == childId }?.sortOrder
        viewModel.reorderCategory(childId, root2Id)
        val after = viewModel.uiState.value.categories.find { it.id == childId }?.sortOrder
        assertEquals(before, after)
    }

    @Test
    fun `reorderCategory does nothing when moved onto itself`() {
        viewModel.addCategory("A")
        viewModel.addCategory("B")
        val aId = viewModel.uiState.value.categories.first { it.name == "A" }.id
        val beforeA = viewModel.uiState.value.categories.find { it.id == aId }?.sortOrder
        viewModel.reorderCategory(aId, aId)
        val afterA = viewModel.uiState.value.categories.find { it.id == aId }?.sortOrder
        assertEquals(beforeA, afterA)
    }

    // --- moveCategory ---

    @Test
    fun `moveCategory reparents to new parent`() {
        viewModel.addCategory("Work")
        viewModel.addCategory("Family")
        val workId = viewModel.uiState.value.categories.first { it.name == "Work" }.id
        val familyId = viewModel.uiState.value.categories.first { it.name == "Family" }.id
        viewModel.addCategory("Engineering", workId)
        val engineeringId = viewModel.uiState.value.categories.first { it.name == "Engineering" }.id

        viewModel.moveCategory(engineeringId, familyId, null)

        val engineering = viewModel.uiState.value.categories.find { it.id == engineeringId }
        assertEquals(familyId, engineering?.parentId)
    }

    @Test
    fun `moveCategory promotes child to root`() {
        viewModel.addCategory("Work")
        val workId = viewModel.uiState.value.categories.first { it.name == "Work" }.id
        viewModel.addCategory("Engineering", workId)
        val engineeringId = viewModel.uiState.value.categories.first { it.name == "Engineering" }.id

        viewModel.moveCategory(engineeringId, null, null)

        val engineering = viewModel.uiState.value.categories.find { it.id == engineeringId }
        assertNull(engineering?.parentId)
    }

    @Test
    fun `moveCategory respects targetPositionId among new siblings`() {
        viewModel.addCategory("A")
        viewModel.addCategory("B")
        viewModel.addCategory("C")
        viewModel.addCategory("Work")
        val aId = viewModel.uiState.value.categories.first { it.name == "A" }.id
        val bId = viewModel.uiState.value.categories.first { it.name == "B" }.id
        val workId = viewModel.uiState.value.categories.first { it.name == "Work" }.id

        viewModel.addCategory("Engineering", workId)
        val engineeringId = viewModel.uiState.value.categories.first { it.name == "Engineering" }.id

        // Move Engineering to root, inserting before B
        viewModel.moveCategory(engineeringId, null, bId)

        val rootCats = viewModel.uiState.value.categories
            .filter { it.parentId == null }
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
            .map { it.name }
        assertEquals(listOf("A", "Engineering", "B", "C", "Work"), rootCats)
    }

    @Test
    fun `moveCategory does nothing when new parent is self`() {
        viewModel.addCategory("Work")
        val workId = viewModel.uiState.value.categories.first { it.name == "Work" }.id
        val beforeParent = viewModel.uiState.value.categories.find { it.id == workId }?.parentId

        viewModel.moveCategory(workId, workId, null)

        val afterParent = viewModel.uiState.value.categories.find { it.id == workId }?.parentId
        assertEquals(beforeParent, afterParent)
    }

    @Test
    fun `moveCategory prevents cycle - cannot move parent under its own descendant`() {
        viewModel.addCategory("Work")
        val workId = viewModel.uiState.value.categories.first { it.name == "Work" }.id
        viewModel.addCategory("Engineering", workId)
        val engineeringId = viewModel.uiState.value.categories.first { it.name == "Engineering" }.id

        viewModel.moveCategory(workId, engineeringId, null)

        val work = viewModel.uiState.value.categories.find { it.id == workId }
        assertNull(work?.parentId)
    }

    @Test
    fun `moveCategory reorders old parent siblings after removal`() {
        viewModel.addCategory("Work")
        val workId = viewModel.uiState.value.categories.first { it.name == "Work" }.id
        viewModel.addCategory("Family")
        val familyId = viewModel.uiState.value.categories.first { it.name == "Family" }.id
        viewModel.addCategory("Child1", workId)
        viewModel.addCategory("Child2", workId)
        viewModel.addCategory("Child3", workId)
        val child1Id = viewModel.uiState.value.categories.first { it.name == "Child1" }.id

        viewModel.moveCategory(child1Id, familyId, null)

        val workChildren = viewModel.uiState.value.categories
            .filter { it.parentId == workId }
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
            .map { it.name }
        assertEquals(listOf("Child2", "Child3"), workChildren)
        val child2 = viewModel.uiState.value.categories.first { it.name == "Child2" }
        val child3 = viewModel.uiState.value.categories.first { it.name == "Child3" }
        assertTrue(child2.sortOrder < child3.sortOrder)
    }
}
