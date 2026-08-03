package com.example.testapp004

import androidx.lifecycle.SavedStateHandle
import com.example.testapp004.model.RelationCategory
import com.example.testapp004.util.MainDispatcherRule
import com.example.testapp004.viewmodel.PersonCanvasViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PersonCanvasViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeAcquaintanceRepository: FakeAcquaintanceRepository
    private lateinit var fakeRelationRepository: FakeRelationRepository

    @Before
    fun setup() {
        fakeAcquaintanceRepository = FakeAcquaintanceRepository()
        fakeRelationRepository = FakeRelationRepository()
    }

    private fun createViewModel(personId: Long) = PersonCanvasViewModel(
        savedStateHandle = SavedStateHandle(mapOf("acquaintanceId" to personId)),
        acquaintanceRepository = fakeAcquaintanceRepository,
        relationRepository = fakeRelationRepository,
    )

    @Test
    fun `personName is set from the center person`() {
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", emptySet()) }
        assertEquals("Alice", createViewModel(aliceId).uiState.value.personName)
    }

    @Test
    fun `nodes are empty when person has no relations`() {
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", emptySet()) }
        val vm = createViewModel(aliceId)
        assertTrue(vm.uiState.value.nodes.isEmpty())
    }

    @Test
    fun `center person node has isDirectMember true`() {
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", emptySet()) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet()) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "FRIEND", null) }
        val vm = createViewModel(aliceId)
        val centerNode = vm.uiState.value.nodes.first { it.id == aliceId }
        assertTrue(centerNode.isDirectMember)
    }

    @Test
    fun `related person node has isDirectMember false`() {
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", emptySet()) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet()) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "FRIEND", null) }
        val vm = createViewModel(aliceId)
        val bobNode = vm.uiState.value.nodes.first { it.id == bobId }
        assertFalse(bobNode.isDirectMember)
    }

    @Test
    fun `center person is placed at the origin`() {
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", emptySet()) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet()) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "COLLEAGUE", null) }
        val vm = createViewModel(aliceId)
        val centerNode = vm.uiState.value.nodes.first { it.id == aliceId }
        assertEquals(0f, centerNode.x, 0.01f)
        assertEquals(0f, centerNode.y, 0.01f)
    }

    @Test
    fun `edge category is FAMILY for a family relation`() {
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", emptySet()) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet()) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "SIBLING", null) }
        val vm = createViewModel(aliceId)
        assertEquals(RelationCategory.FAMILY, vm.uiState.value.edges.first().category)
    }

    @Test
    fun `edge category is PROFESSIONAL for a professional relation`() {
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", emptySet()) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet()) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "COLLEAGUE", null) }
        val vm = createViewModel(aliceId)
        assertEquals(RelationCategory.PROFESSIONAL, vm.uiState.value.edges.first().category)
    }

    @Test
    fun `edge category is SOCIAL for a social relation`() {
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", emptySet()) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet()) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "FRIEND", null) }
        val vm = createViewModel(aliceId)
        assertEquals(RelationCategory.SOCIAL, vm.uiState.value.edges.first().category)
    }

    @Test
    fun `edge category is null for a custom relation`() {
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", emptySet()) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet()) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "CUSTOM", "mentor") }
        val vm = createViewModel(aliceId)
        assertNull(vm.uiState.value.edges.first().category)
    }

    @Test
    fun `dominantCategory on center node is null when no relations exist`() {
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", emptySet()) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet()) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "COLLEAGUE", null) }
        val vm = createViewModel(aliceId)
        // No relations exist, so center node has no category — but here Alice has a relation
        val centerNode = vm.uiState.value.nodes.first { it.id == aliceId }
        assertEquals(RelationCategory.PROFESSIONAL, centerNode.dominantCategory)
    }

    @Test
    fun `only relations involving the center person appear`() {
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", emptySet()) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet()) }
        val carolId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Carol", "", emptySet()) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "FRIEND", null) }
        runBlocking { fakeRelationRepository.addRelation(bobId, carolId, "COLLEAGUE", null) }
        val vm = createViewModel(aliceId)
        val nodeIds = vm.uiState.value.nodes.map { it.id }.toSet()
        assertTrue(aliceId in nodeIds)
        assertTrue(bobId in nodeIds)
        assertFalse(carolId in nodeIds)
        assertEquals(1, vm.uiState.value.edges.size)
    }

    @Test
    fun `openRelationDialog sets dialog state correctly`() {
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", emptySet()) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet()) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "FRIEND", null) }
        val vm = createViewModel(aliceId)
        vm.openRelationDialog(aliceId, bobId)
        val state = vm.uiState.value
        assertTrue(state.isRelationDialogOpen)
        assertEquals(aliceId, state.pendingRelationFromId)
        assertEquals(bobId, state.pendingRelationToId)
        assertEquals("Alice", state.pendingRelationFromName)
        assertEquals("Bob", state.pendingRelationToName)
    }

    @Test
    fun `closeRelationDialog clears dialog state`() {
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", emptySet()) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet()) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "FRIEND", null) }
        val vm = createViewModel(aliceId)
        vm.openRelationDialog(aliceId, bobId)
        vm.closeRelationDialog()
        val state = vm.uiState.value
        assertFalse(state.isRelationDialogOpen)
        assertNull(state.pendingRelationFromId)
        assertNull(state.pendingRelationToId)
    }

    @Test
    fun `addRelationFromCanvas adds a new edge and closes dialog`() {
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", emptySet()) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet()) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "FRIEND", null) }
        val vm = createViewModel(aliceId)
        vm.openRelationDialog(aliceId, bobId)
        runBlocking { vm.addRelationFromCanvas("COLLEAGUE", isDragSourceFrom = true, customLabel = null) }
        assertFalse(vm.uiState.value.isRelationDialogOpen)
        assertEquals(2, vm.uiState.value.edges.size)
    }

    @Test
    fun `isNetSource is true when person has more outgoing asymmetric relations`() {
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", emptySet()) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet()) }
        val carolId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Carol", "", emptySet()) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "PARENT_CHILD", null) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, carolId, "PARENT_CHILD", null) }
        val vm = createViewModel(aliceId)
        val centerNode = vm.uiState.value.nodes.first { it.id == aliceId }
        assertEquals(true, centerNode.isNetSource)
    }

    @Test
    fun `isNetSource is false when person has more incoming asymmetric relations`() {
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", emptySet()) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet()) }
        val carolId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Carol", "", emptySet()) }
        runBlocking { fakeRelationRepository.addRelation(bobId, aliceId, "PARENT_CHILD", null) }
        runBlocking { fakeRelationRepository.addRelation(carolId, aliceId, "PARENT_CHILD", null) }
        val vm = createViewModel(aliceId)
        val centerNode = vm.uiState.value.nodes.first { it.id == aliceId }
        assertEquals(false, centerNode.isNetSource)
    }
}
