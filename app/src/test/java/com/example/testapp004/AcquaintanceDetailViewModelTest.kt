package com.example.testapp004

import androidx.lifecycle.SavedStateHandle
import com.example.testapp004.util.MainDispatcherRule
import com.example.testapp004.viewmodel.AcquaintanceDetailViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AcquaintanceDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeAcquaintanceRepository: FakeAcquaintanceRepository
    private lateinit var fakeCategoryRepository: FakeCategoryRepository
    private lateinit var fakeRelationRepository: FakeRelationRepository
    private var aliceId = 0L

    @Before
    fun setup() {
        fakeAcquaintanceRepository = FakeAcquaintanceRepository()
        fakeCategoryRepository = FakeCategoryRepository()
        fakeRelationRepository = FakeRelationRepository()
        runBlocking {
            aliceId = fakeAcquaintanceRepository.addAcquaintance("Alice", "A friend", null)
        }
    }

    private fun buildViewModel() = AcquaintanceDetailViewModel(
        acquaintanceRepository = fakeAcquaintanceRepository,
        categoryRepository = fakeCategoryRepository,
        relationRepository = fakeRelationRepository,
        savedStateHandle = SavedStateHandle(mapOf("acquaintanceId" to aliceId)),
    )

    @Test
    fun `initial state has no add relation dialog open`() {
        assertFalse(buildViewModel().uiState.value.isAddRelationDialogOpen)
    }

    @Test
    fun `acquaintance is loaded into state`() {
        val vm = buildViewModel()
        assertNotNull(vm.uiState.value.acquaintance)
        assertEquals("Alice", vm.uiState.value.acquaintance?.name)
    }

    @Test
    fun `categoryName is null when no category assigned`() {
        assertNull(buildViewModel().uiState.value.categoryName)
    }

    @Test
    fun `categoryName reflects the assigned category`() {
        runBlocking {
            val catId = fakeCategoryRepository.addCategory("Friends")
            fakeAcquaintanceRepository.updateAcquaintance(
                fakeAcquaintanceRepository.getAcquaintance(aliceId)!!.copy(categoryId = catId),
            )
        }
        assertEquals("Friends", buildViewModel().uiState.value.categoryName)
    }

    @Test
    fun `openAddRelationDialog sets flag to true`() {
        val vm = buildViewModel()
        vm.openAddRelationDialog()
        assertTrue(vm.uiState.value.isAddRelationDialogOpen)
    }

    @Test
    fun `closeAddRelationDialog sets flag to false`() {
        val vm = buildViewModel()
        vm.openAddRelationDialog()
        vm.closeAddRelationDialog()
        assertFalse(vm.uiState.value.isAddRelationDialogOpen)
    }

    @Test
    fun `addRelation creates a relation and closes dialog`() {
        var bobId: Long
        runBlocking { bobId = fakeAcquaintanceRepository.addAcquaintance("Bob", "", null) }
        val vm = buildViewModel()
        vm.openAddRelationDialog()
        vm.addRelation(bobId, "works with")
        assertEquals(1, vm.uiState.value.relations.size)
        assertFalse(vm.uiState.value.isAddRelationDialogOpen)
    }

    @Test
    fun `addRelation with blank label does nothing`() {
        var bobId: Long
        runBlocking { bobId = fakeAcquaintanceRepository.addAcquaintance("Bob", "", null) }
        val vm = buildViewModel()
        vm.addRelation(bobId, "   ")
        assertTrue(vm.uiState.value.relations.isEmpty())
    }

    @Test
    fun `outgoing relation shows isOutgoing true`() {
        var bobId: Long
        runBlocking { bobId = fakeAcquaintanceRepository.addAcquaintance("Bob", "", null) }
        val vm = buildViewModel()
        vm.addRelation(bobId, "mentors")
        assertTrue(vm.uiState.value.relations.first().isOutgoing)
    }

    @Test
    fun `incoming relation shows isOutgoing false`() {
        runBlocking {
            val bobId = fakeAcquaintanceRepository.addAcquaintance("Bob", "", null)
            fakeRelationRepository.addRelation(fromId = bobId, toId = aliceId, label = "mentors")
        }
        assertFalse(buildViewModel().uiState.value.relations.first().isOutgoing)
    }

    @Test
    fun `deleteRelation removes the relation`() {
        var bobId: Long
        runBlocking { bobId = fakeAcquaintanceRepository.addAcquaintance("Bob", "", null) }
        val vm = buildViewModel()
        vm.addRelation(bobId, "knows")
        val relationId = vm.uiState.value.relations.first().relationId
        vm.deleteRelation(relationId)
        assertTrue(vm.uiState.value.relations.isEmpty())
    }

    @Test
    fun `allOtherAcquaintances excludes the current person`() {
        runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", null) }
        val vm = buildViewModel()
        val ids = vm.uiState.value.allOtherAcquaintances.map { it.id }
        assertFalse(ids.contains(aliceId))
        assertEquals(1, ids.size)
    }
}
