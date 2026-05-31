package com.example.testapp004

import com.example.testapp004.util.MainDispatcherRule
import com.example.testapp004.viewmodel.CategoriesViewModel
import kotlinx.coroutines.runBlocking
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
    fun `openEditDialog sets editingCategory and isEditDialogOpen`() {
        runBlocking { fakeCategoryRepository.addCategory("Work") }
        val cat = viewModel.uiState.value.categories.first()
        viewModel.openEditDialog(cat)
        assertEquals(cat, viewModel.uiState.value.editingCategory)
        assertTrue(viewModel.uiState.value.isEditDialogOpen)
    }

    @Test
    fun `closeEditDialog clears editingCategory and closes dialog`() {
        runBlocking { fakeCategoryRepository.addCategory("Work") }
        val cat = viewModel.uiState.value.categories.first()
        viewModel.openEditDialog(cat)
        viewModel.closeEditDialog()
        assertNull(viewModel.uiState.value.editingCategory)
        assertFalse(viewModel.uiState.value.isEditDialogOpen)
    }

    @Test
    fun `editCategory updates category name and closes dialog`() {
        runBlocking { fakeCategoryRepository.addCategory("Work") }
        val cat = viewModel.uiState.value.categories.first()
        viewModel.openEditDialog(cat)
        viewModel.editCategory(cat.id, "Career", null)
        assertEquals("Career", viewModel.uiState.value.categories.first().name)
        assertFalse(viewModel.uiState.value.isEditDialogOpen)
        assertNull(viewModel.uiState.value.editingCategory)
    }

    @Test
    fun `editCategory trims whitespace`() {
        runBlocking { fakeCategoryRepository.addCategory("Work") }
        val cat = viewModel.uiState.value.categories.first()
        viewModel.editCategory(cat.id, "  Career  ", null)
        assertEquals("Career", viewModel.uiState.value.categories.first().name)
    }

    @Test
    fun `editCategory with blank name does nothing`() {
        runBlocking { fakeCategoryRepository.addCategory("Work") }
        val cat = viewModel.uiState.value.categories.first()
        viewModel.editCategory(cat.id, "   ", null)
        assertEquals("Work", viewModel.uiState.value.categories.first().name)
    }

    @Test
    fun `editCategory can change parent`() {
        runBlocking {
            fakeCategoryRepository.addCategory("Work")
            fakeCategoryRepository.addCategory("Family")
        }
        val work = viewModel.uiState.value.categories.find { it.name == "Work" }!!
        val family = viewModel.uiState.value.categories.find { it.name == "Family" }!!
        viewModel.editCategory(work.id, "Work", family.id)
        val updated = viewModel.uiState.value.categories.find { it.id == work.id }!!
        assertEquals(family.id, updated.parentId)
    }
}
