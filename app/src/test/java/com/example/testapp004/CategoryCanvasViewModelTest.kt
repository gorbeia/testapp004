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

    @Test
    fun `edge category is FAMILY for a family relation type`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId)) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "SIBLING", null) }
        assertEquals(RelationCategory.FAMILY, createViewModel(catId).uiState.value.edges.first().category)
    }

    @Test
    fun `edge category is PROFESSIONAL for a professional relation type`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId)) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "COLLEAGUE", null) }
        assertEquals(RelationCategory.PROFESSIONAL, createViewModel(catId).uiState.value.edges.first().category)
    }

    @Test
    fun `edge category is SOCIAL for a social relation type`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId)) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "FRIEND", null) }
        assertEquals(RelationCategory.SOCIAL, createViewModel(catId).uiState.value.edges.first().category)
    }

    @Test
    fun `edge category is null for a custom relation`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId)) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "CUSTOM", "mentor") }
        assertNull(createViewModel(catId).uiState.value.edges.first().category)
    }

    @Test
    fun `node dominantCategory is null when person has no relations`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        assertNull(createViewModel(catId).uiState.value.nodes.first().dominantCategory)
    }

    @Test
    fun `node dominantCategory matches sole relation category`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId)) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "FRIEND", null) }
        val vm = createViewModel(catId)
        assertEquals(RelationCategory.SOCIAL, vm.uiState.value.nodes.first { it.name == "Alice" }.dominantCategory)
    }

    @Test
    fun `node dominantCategory is most frequent category for mixed relations`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId)) }
        val carolId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Carol", "", setOf(catId)) }
        val daveId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Dave", "", setOf(catId)) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "SIBLING", null) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, carolId, "COUSIN", null) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, daveId, "COLLEAGUE", null) }
        val alice = createViewModel(catId).uiState.value.nodes.first { it.name == "Alice" }
        assertEquals(RelationCategory.FAMILY, alice.dominantCategory)
    }

    @Test
    fun `both endpoints of a relation receive the same dominantCategory`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId)) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "SPOUSE", null) }
        val vm = createViewModel(catId)
        assertEquals(RelationCategory.FAMILY, vm.uiState.value.nodes.first { it.name == "Alice" }.dominantCategory)
        assertEquals(RelationCategory.FAMILY, vm.uiState.value.nodes.first { it.name == "Bob" }.dominantCategory)
    }

    @Test
    fun `custom-only relations leave node dominantCategory null`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId)) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "CUSTOM", "co-founder") }
        assertNull(createViewModel(catId).uiState.value.nodes.first { it.name == "Alice" }.dominantCategory)
    }

    @Test
    fun `node isNetSource is null when person has no relations`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        assertNull(createViewModel(catId).uiState.value.nodes.first { it.name == "Alice" }.isNetSource)
    }

    @Test
    fun `node isNetSource is null for both sides of a symmetric relation`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId)) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "SPOUSE", null) }
        val vm = createViewModel(catId)
        assertNull(vm.uiState.value.nodes.first { it.name == "Alice" }.isNetSource)
        assertNull(vm.uiState.value.nodes.first { it.name == "Bob" }.isNetSource)
    }

    @Test
    fun `node isNetSource ignores symmetric relations when mixed with asymmetric`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId)) }
        val carolId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Carol", "", setOf(catId)) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "SPOUSE", null) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, carolId, "PARENT_CHILD", null) }
        val vm = createViewModel(catId)
        assertEquals(true, vm.uiState.value.nodes.first { it.name == "Alice" }.isNetSource)
    }

    @Test
    fun `node isNetSource is true for the from-side of a directed relation`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId)) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "PARENT_CHILD", null) }
        val vm = createViewModel(catId)
        assertEquals(true, vm.uiState.value.nodes.first { it.name == "Alice" }.isNetSource)
    }

    @Test
    fun `node isNetSource is false for the to-side of a directed relation`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId)) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "PARENT_CHILD", null) }
        val vm = createViewModel(catId)
        assertEquals(false, vm.uiState.value.nodes.first { it.name == "Bob" }.isNetSource)
    }

    @Test
    fun `node isNetSource is null when from and to counts are equal`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId)) }
        val carolId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Carol", "", setOf(catId)) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "PARENT_CHILD", null) }
        runBlocking { fakeRelationRepository.addRelation(carolId, aliceId, "PARENT_CHILD", null) }
        val vm = createViewModel(catId)
        assertNull(vm.uiState.value.nodes.first { it.name == "Alice" }.isNetSource)
    }

    @Test
    fun `node isNetSource reflects net direction when mixed from and to relations`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId)) }
        val carolId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Carol", "", setOf(catId)) }
        val daveId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Dave", "", setOf(catId)) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "PARENT_CHILD", null) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, carolId, "PARENT_CHILD", null) }
        runBlocking { fakeRelationRepository.addRelation(daveId, aliceId, "PARENT_CHILD", null) }
        val vm = createViewModel(catId)
        assertEquals(true, vm.uiState.value.nodes.first { it.name == "Alice" }.isNetSource)
    }

    @Test
    fun `node isDirectMember is true when person belongs directly to canvas category`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Parent") }
        runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val alice = createViewModel(catId).uiState.value.nodes.first { it.name == "Alice" }
        assertEquals(true, alice.isDirectMember)
    }

    @Test
    fun `node isDirectMember is false when person belongs only to child category`() {
        val parentId = runBlocking { fakeCategoryRepository.addCategory("Parent") }
        val childId = runBlocking { fakeCategoryRepository.addCategory("Child", parentId) }
        runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(childId)) }
        val bob = createViewModel(parentId).uiState.value.nodes.first { it.name == "Bob" }
        assertEquals(false, bob.isDirectMember)
    }

    @Test
    fun `node isDirectMember is true when person belongs to both parent and child category`() {
        val parentId = runBlocking { fakeCategoryRepository.addCategory("Parent") }
        val childId = runBlocking { fakeCategoryRepository.addCategory("Child", parentId) }
        runBlocking { fakeAcquaintanceRepository.addAcquaintance("Carol", "", setOf(parentId, childId)) }
        val carol = createViewModel(parentId).uiState.value.nodes.first { it.name == "Carol" }
        assertEquals(true, carol.isDirectMember)
    }

    @Test
    fun `category member node has distanceFromCategory of 0`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val alice = createViewModel(catId).uiState.value.nodes.first { it.name == "Alice" }
        assertEquals(0, alice.distanceFromCategory)
    }

    @Test
    fun `at distance 0 person related to category member but outside category is excluded`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val charlieId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Charlie", "", emptySet()) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, charlieId, "FRIEND", null) }
        val vm = createViewModel(catId)
        assert(vm.uiState.value.nodes.none { it.name == "Charlie" })
    }

    @Test
    fun `setRelationDistance updates uiState relationDistance`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val vm = createViewModel(catId)
        vm.setRelationDistance(1)
        assertEquals(1, vm.uiState.value.relationDistance)
    }

    @Test
    fun `at distance 1 person related to category member is included`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val charlieId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Charlie", "", emptySet()) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, charlieId, "FRIEND", null) }
        val vm = createViewModel(catId)
        vm.setRelationDistance(1)
        assert(vm.uiState.value.nodes.any { it.name == "Charlie" })
    }

    @Test
    fun `at distance 1 included person has distanceFromCategory of 1`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val charlieId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Charlie", "", emptySet()) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, charlieId, "FRIEND", null) }
        val vm = createViewModel(catId)
        vm.setRelationDistance(1)
        val charlie = vm.uiState.value.nodes.first { it.name == "Charlie" }
        assertEquals(1, charlie.distanceFromCategory)
    }

    @Test
    fun `at distance 1 person two hops from category is not included`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val charlieId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Charlie", "", emptySet()) }
        val daveId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Dave", "", emptySet()) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, charlieId, "FRIEND", null) }
        runBlocking { fakeRelationRepository.addRelation(charlieId, daveId, "FRIEND", null) }
        val vm = createViewModel(catId)
        vm.setRelationDistance(1)
        assert(vm.uiState.value.nodes.none { it.name == "Dave" })
    }

    @Test
    fun `at distance 2 person two hops from category is included`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val charlieId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Charlie", "", emptySet()) }
        val daveId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Dave", "", emptySet()) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, charlieId, "FRIEND", null) }
        runBlocking { fakeRelationRepository.addRelation(charlieId, daveId, "FRIEND", null) }
        val vm = createViewModel(catId)
        vm.setRelationDistance(2)
        assert(vm.uiState.value.nodes.any { it.name == "Dave" })
    }

    @Test
    fun `at distance 2 two-hop node has distanceFromCategory of 2`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val charlieId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Charlie", "", emptySet()) }
        val daveId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Dave", "", emptySet()) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, charlieId, "FRIEND", null) }
        runBlocking { fakeRelationRepository.addRelation(charlieId, daveId, "FRIEND", null) }
        val vm = createViewModel(catId)
        vm.setRelationDistance(2)
        val dave = vm.uiState.value.nodes.first { it.name == "Dave" }
        assertEquals(2, dave.distanceFromCategory)
    }

    @Test
    fun `at distance 1 edges between category member and distance-1 node are shown`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val charlieId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Charlie", "", emptySet()) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, charlieId, "FRIEND", null) }
        val vm = createViewModel(catId)
        vm.setRelationDistance(1)
        assert(vm.uiState.value.edges.any { it.fromId == aliceId && it.toId == charlieId })
    }

    @Test
    fun `symmetric relation type produces edge with isSymmetric true`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId)) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "SPOUSE", null) }
        assertEquals(true, createViewModel(catId).uiState.value.edges.first().isSymmetric)
    }

    @Test
    fun `asymmetric relation type produces edge with isSymmetric false`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId)) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "PARENT_CHILD", null) }
        assertEquals(false, createViewModel(catId).uiState.value.edges.first().isSymmetric)
    }

    @Test
    fun `custom relation type produces edge with isSymmetric false`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId)) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "CUSTOM", "co-founder") }
        assertEquals(false, createViewModel(catId).uiState.value.edges.first().isSymmetric)
    }

    @Test
    fun `toggleRelationCategoryFilter adds category to empty filter`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val vm = createViewModel(catId)
        vm.toggleRelationCategoryFilter(RelationCategory.FAMILY)
        assertEquals(setOf(RelationCategory.FAMILY), vm.uiState.value.relationCategoryFilter)
    }

    @Test
    fun `toggleRelationCategoryFilter removes category when already active`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val vm = createViewModel(catId)
        vm.toggleRelationCategoryFilter(RelationCategory.FAMILY)
        vm.toggleRelationCategoryFilter(RelationCategory.FAMILY)
        assertEquals(emptySet<RelationCategory>(), vm.uiState.value.relationCategoryFilter)
    }

    @Test
    fun `toggleRelationCategoryFilter allows multiple categories to be active simultaneously`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val vm = createViewModel(catId)
        vm.toggleRelationCategoryFilter(RelationCategory.FAMILY)
        vm.toggleRelationCategoryFilter(RelationCategory.SOCIAL)
        assertEquals(
            setOf(RelationCategory.FAMILY, RelationCategory.SOCIAL),
            vm.uiState.value.relationCategoryFilter,
        )
    }

    @Test
    fun `with FAMILY filter active, FAMILY relation edges are shown`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId)) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "SIBLING", null) }
        val vm = createViewModel(catId)
        vm.toggleRelationCategoryFilter(RelationCategory.FAMILY)
        assert(vm.uiState.value.edges.any { it.fromId == aliceId && it.toId == bobId })
    }

    @Test
    fun `with FAMILY filter active, SOCIAL relation edges are hidden`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", setOf(catId)) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, bobId, "FRIEND", null) }
        val vm = createViewModel(catId)
        vm.toggleRelationCategoryFilter(RelationCategory.FAMILY)
        assert(vm.uiState.value.edges.none { it.fromId == aliceId && it.toId == bobId })
    }

    @Test
    fun `with FAMILY filter active, direct category members are always shown`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val vm = createViewModel(catId)
        vm.toggleRelationCategoryFilter(RelationCategory.FAMILY)
        assert(vm.uiState.value.nodes.any { it.name == "Alice" })
    }

    @Test
    fun `with FAMILY filter active, distance-1 node reachable only via SOCIAL is excluded`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val charlieId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Charlie", "", emptySet()) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, charlieId, "FRIEND", null) }
        val vm = createViewModel(catId)
        vm.setRelationDistance(1)
        vm.toggleRelationCategoryFilter(RelationCategory.FAMILY)
        assert(vm.uiState.value.nodes.none { it.name == "Charlie" })
    }

    @Test
    fun `with FAMILY filter active, distance-1 node reachable via FAMILY is included`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Test") }
        val aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "", setOf(catId)) }
        val charlieId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Charlie", "", emptySet()) }
        runBlocking { fakeRelationRepository.addRelation(aliceId, charlieId, "SIBLING", null) }
        val vm = createViewModel(catId)
        vm.setRelationDistance(1)
        vm.toggleRelationCategoryFilter(RelationCategory.FAMILY)
        assert(vm.uiState.value.nodes.any { it.name == "Charlie" })
    }
}
