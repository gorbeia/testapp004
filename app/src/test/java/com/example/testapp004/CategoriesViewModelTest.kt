package com.example.testapp004

import com.example.testapp004.util.MainDispatcherRule
import com.example.testapp004.viewmodel.CategoriesViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `categories assigned unique ids`() {
        viewModel.addCategory("Work")
        viewModel.addCategory("Family")
        val ids = viewModel.uiState.value.categories.map { it.id }
        assertEquals(ids.distinct(), ids)
    }
}
