package com.example.testapp004

import androidx.lifecycle.SavedStateHandle
import com.example.testapp004.util.MainDispatcherRule
import com.example.testapp004.viewmodel.AddEditAcquaintanceViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AddEditAcquaintanceViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeAcquaintanceRepository: FakeAcquaintanceRepository
    private lateinit var fakeCategoryRepository: FakeCategoryRepository

    @Before
    fun setup() {
        fakeAcquaintanceRepository = FakeAcquaintanceRepository()
        fakeCategoryRepository = FakeCategoryRepository()
    }

    private fun buildViewModel(acquaintanceId: Long = -1L) = AddEditAcquaintanceViewModel(
        acquaintanceRepository = fakeAcquaintanceRepository,
        categoryRepository = fakeCategoryRepository,
        savedStateHandle = SavedStateHandle(mapOf("acquaintanceId" to acquaintanceId)),
    )

    @Test
    fun `initial state is empty for new person`() {
        val vm = buildViewModel()
        assertEquals("", vm.uiState.value.name)
        assertEquals("", vm.uiState.value.bio)
        assertFalse(vm.uiState.value.isEditing)
        assertFalse(vm.uiState.value.isSaved)
    }

    @Test
    fun `isEditing is true when acquaintanceId provided`() {
        val id = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "Bio", null) }
        assertTrue(buildViewModel(id).uiState.value.isEditing)
    }

    @Test
    fun `edit mode loads existing acquaintance data`() {
        val id = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "A friend", null) }
        val vm = buildViewModel(id)
        assertEquals("Alice", vm.uiState.value.name)
        assertEquals("A friend", vm.uiState.value.bio)
    }

    @Test
    fun `onNameChange updates name in state`() {
        val vm = buildViewModel()
        vm.onNameChange("Bob")
        assertEquals("Bob", vm.uiState.value.name)
    }

    @Test
    fun `onBioChange updates bio in state`() {
        val vm = buildViewModel()
        vm.onBioChange("Met at work")
        assertEquals("Met at work", vm.uiState.value.bio)
    }

    @Test
    fun `save with blank name does nothing`() {
        val vm = buildViewModel()
        vm.onNameChange("   ")
        vm.save()
        assertFalse(vm.uiState.value.isSaved)
        assertTrue(fakeAcquaintanceRepository.acquaintances.value.isEmpty())
    }

    @Test
    fun `save creates new acquaintance and sets isSaved`() {
        val vm = buildViewModel()
        vm.onNameChange("Charlie")
        vm.onBioChange("Old friend")
        vm.save()
        assertTrue(vm.uiState.value.isSaved)
        assertEquals(1, fakeAcquaintanceRepository.acquaintances.value.size)
        assertEquals("Charlie", fakeAcquaintanceRepository.acquaintances.value.first().name)
    }

    @Test
    fun `save trims whitespace from name and bio`() {
        val vm = buildViewModel()
        vm.onNameChange("  Dave  ")
        vm.onBioChange("  College friend  ")
        vm.save()
        val saved = fakeAcquaintanceRepository.acquaintances.value.first()
        assertEquals("Dave", saved.name)
        assertEquals("College friend", saved.bio)
    }

    @Test
    fun `save in edit mode updates existing acquaintance without creating a new one`() {
        val id = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "Old bio", null) }
        val vm = buildViewModel(id)
        vm.onBioChange("New bio")
        vm.save()
        assertEquals(1, fakeAcquaintanceRepository.acquaintances.value.size)
        assertEquals("New bio", fakeAcquaintanceRepository.acquaintances.value.first().bio)
    }

    @Test
    fun `categories from repository are reflected in state`() {
        runBlocking { fakeCategoryRepository.addCategory("Work") }
        assertEquals(1, buildViewModel().uiState.value.categories.size)
    }

    @Test
    fun `onCategorySelected updates selectedCategoryId`() {
        val vm = buildViewModel()
        vm.onCategorySelected(42L)
        assertEquals(42L, vm.uiState.value.selectedCategoryId)
    }

    @Test
    fun `onCategorySelected null clears selectedCategoryId`() {
        val vm = buildViewModel()
        vm.onCategorySelected(42L)
        vm.onCategorySelected(null)
        assertEquals(null, vm.uiState.value.selectedCategoryId)
    }
}
