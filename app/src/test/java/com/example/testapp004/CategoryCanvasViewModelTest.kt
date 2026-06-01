package com.example.testapp004

import androidx.lifecycle.SavedStateHandle
import com.example.testapp004.model.RelationCategory
import com.example.testapp004.util.MainDispatcherRule
import com.example.testapp004.viewmodel.CategoryCanvasViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CategoryCanvasViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeAcquaintanceRepository: FakeAcquaintanceRepository
    private lateinit var fakeCategoryRepository: FakeCategoryRepository
    private lateinit var fakeRelationRepository: FakeRelationRepository

    @Before
    fun setup() {
        fakeAcquaintanceRepository = FakeAcquaintanceRepository()
        fakeCategoryRepository = FakeCategoryRepository()
        fakeRelationRepository = FakeRelationRepository()
    }

    private fun createViewModel(categoryId: Long) = CategoryCanvasViewModel(
        savedStateHandle = SavedStateHandle(mapOf("categoryId" to categoryId)),
        acquaintanceRepository = fakeAcquaintanceRepository,
        categoryRepository = fakeCategoryRepository,
        relationRepository = fakeRelationRepository,
    )

    // --- Edge category ---

    @Test
    fun `edge category is FAMILY for a family relation type`() = runBlocking {
        val catId = fakeCategoryRepository.addCategory("Test", null)
        val aliceId = fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId))
        val bobId = fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId))
        fakeRelationRepository.addRelation(aliceId, bobId, "SIBLING", null)

        val edge = createViewModel(catId).uiState.value.edges.first()
        assertEquals(RelationCategory.FAMILY, edge.category)
    }

    @Test
    fun `edge category is PROFESSIONAL for a professional relation type`() = runBlocking {
        val catId = fakeCategoryRepository.addCategory("Test", null)
        val aliceId = fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId))
        val bobId = fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId))
        fakeRelationRepository.addRelation(aliceId, bobId, "COLLEAGUE", null)

        val edge = createViewModel(catId).uiState.value.edges.first()
        assertEquals(RelationCategory.PROFESSIONAL, edge.category)
    }

    @Test
    fun `edge category is SOCIAL for a social relation type`() = runBlocking {
        val catId = fakeCategoryRepository.addCategory("Test", null)
        val aliceId = fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId))
        val bobId = fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId))
        fakeRelationRepository.addRelation(aliceId, bobId, "FRIEND", null)

        val edge = createViewModel(catId).uiState.value.edges.first()
        assertEquals(RelationCategory.SOCIAL, edge.category)
    }

    @Test
    fun `edge category is null for a custom relation`() = runBlocking {
        val catId = fakeCategoryRepository.addCategory("Test", null)
        val aliceId = fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId))
        val bobId = fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId))
        fakeRelationRepository.addRelation(aliceId, bobId, "CUSTOM", "mentor")

        val edge = createViewModel(catId).uiState.value.edges.first()
        assertNull(edge.category)
    }

    // --- Node dominant category ---

    @Test
    fun `node dominantCategory is null when person has no relations`() = runBlocking {
        val catId = fakeCategoryRepository.addCategory("Test", null)
        fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId))

        val node = createViewModel(catId).uiState.value.nodes.first()
        assertNull(node.dominantCategory)
    }

    @Test
    fun `node dominantCategory matches sole relation category`() = runBlocking {
        val catId = fakeCategoryRepository.addCategory("Test", null)
        val aliceId = fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId))
        val bobId = fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId))
        fakeRelationRepository.addRelation(aliceId, bobId, "FRIEND", null)

        val vm = createViewModel(catId)
        val alice = vm.uiState.value.nodes.first { it.name == "Alice" }
        assertEquals(RelationCategory.SOCIAL, alice.dominantCategory)
    }

    @Test
    fun `node dominantCategory is most frequent category for mixed relations`() = runBlocking {
        val catId = fakeCategoryRepository.addCategory("Test", null)
        val aliceId = fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId))
        val bobId = fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId))
        val carolId = fakeAcquaintanceRepository.addAcquaintance("Carol", "", setOf(catId))
        val daveId = fakeAcquaintanceRepository.addAcquaintance("Dave", "", setOf(catId))
        // Alice: 2 FAMILY edges, 1 PROFESSIONAL → FAMILY is dominant
        fakeRelationRepository.addRelation(aliceId, bobId, "SIBLING", null)
        fakeRelationRepository.addRelation(aliceId, carolId, "COUSIN", null)
        fakeRelationRepository.addRelation(aliceId, daveId, "COLLEAGUE", null)

        val alice = createViewModel(catId).uiState.value.nodes.first { it.name == "Alice" }
        assertEquals(RelationCategory.FAMILY, alice.dominantCategory)
    }

    @Test
    fun `both endpoints of a relation receive the same dominantCategory`() = runBlocking {
        val catId = fakeCategoryRepository.addCategory("Test", null)
        val aliceId = fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId))
        val bobId = fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId))
        fakeRelationRepository.addRelation(aliceId, bobId, "SPOUSE", null)

        val vm = createViewModel(catId)
        val alice = vm.uiState.value.nodes.first { it.name == "Alice" }
        val bob = vm.uiState.value.nodes.first { it.name == "Bob" }
        assertEquals(RelationCategory.FAMILY, alice.dominantCategory)
        assertEquals(RelationCategory.FAMILY, bob.dominantCategory)
    }

    @Test
    fun `custom-only relations leave node dominantCategory null`() = runBlocking {
        val catId = fakeCategoryRepository.addCategory("Test", null)
        val aliceId = fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId))
        val bobId = fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId))
        fakeRelationRepository.addRelation(aliceId, bobId, "CUSTOM", "co-founder")

        val alice = createViewModel(catId).uiState.value.nodes.first { it.name == "Alice" }
        assertNull(alice.dominantCategory)
    }
}
