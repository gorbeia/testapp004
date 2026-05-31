package com.example.testapp004

import com.example.testapp004.util.MainDispatcherRule
import com.example.testapp004.viewmodel.AcquaintancesViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AcquaintancesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeAcquaintanceRepository: FakeAcquaintanceRepository
    private lateinit var fakeCategoryRepository: FakeCategoryRepository

    @Before
    fun setup() {
        fakeAcquaintanceRepository = FakeAcquaintanceRepository()
        fakeCategoryRepository = FakeCategoryRepository()
    }

    private fun createViewModel() = AcquaintancesViewModel(fakeAcquaintanceRepository, fakeCategoryRepository)

    @Test
    fun `initial state has empty acquaintances list`() {
        assertTrue(createViewModel().uiState.value.acquaintances.isEmpty())
    }

    @Test
    fun `initial state has no selected category`() {
        assertNull(createViewModel().uiState.value.selectedCategoryId)
    }

    @Test
    fun `acquaintances from repository appear in state`() {
        runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "Friend", emptySet()) }
        val vm = createViewModel()
        assertEquals(1, vm.uiState.value.acquaintances.size)
        assertEquals("Alice", vm.uiState.value.acquaintances.first().name)
    }

    @Test
    fun `deleteAcquaintance removes from list`() {
        runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", emptySet()) }
        val vm = createViewModel()
        val id = vm.uiState.value.acquaintances.first().id
        vm.deleteAcquaintance(id)
        assertTrue(vm.uiState.value.acquaintances.isEmpty())
    }

    @Test
    fun `deleteAcquaintance only removes targeted person`() {
        runBlocking {
            fakeAcquaintanceRepository.addAcquaintance("Alice", "", emptySet())
            fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet())
        }
        val vm = createViewModel()
        val aliceId = vm.uiState.value.acquaintances.first().id
        vm.deleteAcquaintance(aliceId)
        assertEquals(1, vm.uiState.value.acquaintances.size)
        assertEquals("Bob", vm.uiState.value.acquaintances.first().name)
    }

    @Test
    fun `selectCategory filters acquaintances to matching category`() {
        runBlocking {
            val catId = fakeCategoryRepository.addCategory("Work")
            fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId))
            fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet())
        }
        val vm = createViewModel()
        val catId = vm.uiState.value.categories.first().id
        vm.selectCategory(catId)
        assertEquals(1, vm.uiState.value.acquaintances.size)
        assertEquals("Alice", vm.uiState.value.acquaintances.first().name)
    }

    @Test
    fun `selectCategory null shows all acquaintances`() {
        runBlocking {
            val catId = fakeCategoryRepository.addCategory("Work")
            fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId))
            fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet())
        }
        val vm = createViewModel()
        val catId = vm.uiState.value.categories.first().id
        vm.selectCategory(catId)
        vm.selectCategory(null)
        assertEquals(2, vm.uiState.value.acquaintances.size)
    }

    @Test
    fun `selectCategory includes people in descendant categories`() {
        runBlocking {
            val parentId = fakeCategoryRepository.addCategory("Work")
            val childId = fakeCategoryRepository.addCategory("Engineering", parentId)
            fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(parentId))
            fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(childId))
            fakeAcquaintanceRepository.addAcquaintance("Carol", "", emptySet())
        }
        val vm = createViewModel()
        val parentId = vm.uiState.value.categories.first { it.name == "Work" }.id
        vm.selectCategory(parentId)
        assertEquals(2, vm.uiState.value.acquaintances.size)
        assertTrue(vm.uiState.value.acquaintances.any { it.name == "Alice" })
        assertTrue(vm.uiState.value.acquaintances.any { it.name == "Bob" })
    }

    @Test
    fun `person in multiple categories appears in each category filter`() {
        runBlocking {
            val workId = fakeCategoryRepository.addCategory("Work")
            val familyId = fakeCategoryRepository.addCategory("Family")
            fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(workId, familyId))
        }
        val vm = createViewModel()
        val workId = vm.uiState.value.categories.first { it.name == "Work" }.id
        val familyId = vm.uiState.value.categories.first { it.name == "Family" }.id
        vm.selectCategory(workId)
        assertEquals(1, vm.uiState.value.acquaintances.size)
        vm.selectCategory(familyId)
        assertEquals(1, vm.uiState.value.acquaintances.size)
    }

    @Test
    fun `categories from repository are reflected in state`() {
        runBlocking {
            fakeCategoryRepository.addCategory("Work")
            fakeCategoryRepository.addCategory("Family")
        }
        assertEquals(2, createViewModel().uiState.value.categories.size)
    }
}
