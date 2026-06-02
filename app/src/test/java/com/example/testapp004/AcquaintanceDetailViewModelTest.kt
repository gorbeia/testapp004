package com.example.testapp004

import androidx.lifecycle.SavedStateHandle
import com.example.testapp004.model.RelationTypes
import com.example.testapp004.util.MainDispatcherRule
import com.example.testapp004.viewmodel.AcquaintanceDetailViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    private lateinit var fakeContactRepository: FakeContactRepository
    private var aliceId = 0L

    @Before
    fun setup() {
        fakeAcquaintanceRepository = FakeAcquaintanceRepository()
        fakeCategoryRepository = FakeCategoryRepository()
        fakeRelationRepository = FakeRelationRepository()
        fakeContactRepository = FakeContactRepository()
        aliceId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Alice", "A friend", emptySet()) }
    }

    private fun buildViewModel() = AcquaintanceDetailViewModel(
        acquaintanceRepository = fakeAcquaintanceRepository,
        categoryRepository = fakeCategoryRepository,
        relationRepository = fakeRelationRepository,
        contactRepository = fakeContactRepository,
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
    fun `categoryNames is empty when no categories assigned`() {
        assertTrue(buildViewModel().uiState.value.categoryNames.isEmpty())
    }

    @Test
    fun `categoryNames reflects assigned categories`() {
        val catId = runBlocking { fakeCategoryRepository.addCategory("Friends") }
        runBlocking {
            fakeAcquaintanceRepository.updateAcquaintance(
                fakeAcquaintanceRepository.getAcquaintance(aliceId)!!.copy(categoryIds = setOf(catId)),
            )
        }
        assertTrue(buildViewModel().uiState.value.categoryNames.contains("Friends"))
    }

    @Test
    fun `categoryNames can contain multiple categories`() {
        val workId = runBlocking { fakeCategoryRepository.addCategory("Work") }
        val familyId = runBlocking { fakeCategoryRepository.addCategory("Family") }
        runBlocking {
            fakeAcquaintanceRepository.updateAcquaintance(
                fakeAcquaintanceRepository.getAcquaintance(aliceId)!!.copy(categoryIds = setOf(workId, familyId)),
            )
        }
        val names = buildViewModel().uiState.value.categoryNames
        assertTrue(names.contains("Work"))
        assertTrue(names.contains("Family"))
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
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet()) }
        val vm = buildViewModel()
        vm.openAddRelationDialog()
        vm.addRelation(bobId, RelationTypes.CUSTOM_KEY, isCurrentPersonFrom = true, customLabel = "works with")
        assertEquals(1, vm.uiState.value.relations.size)
        assertFalse(vm.uiState.value.isAddRelationDialogOpen)
    }

    @Test
    fun `addRelation with blank custom label does nothing`() {
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet()) }
        val vm = buildViewModel()
        vm.addRelation(bobId, RelationTypes.CUSTOM_KEY, isCurrentPersonFrom = true, customLabel = "   ")
        assertTrue(vm.uiState.value.relations.isEmpty())
    }

    @Test
    fun `addRelation with predefined type requires no label`() {
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet()) }
        val vm = buildViewModel()
        vm.addRelation(bobId, RelationTypes.FRIEND.key, isCurrentPersonFrom = true, customLabel = null)
        assertEquals(1, vm.uiState.value.relations.size)
    }

    @Test
    fun `outgoing relation shows isOutgoing true`() {
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet()) }
        val vm = buildViewModel()
        vm.addRelation(bobId, RelationTypes.MENTOR_MENTEE.key, isCurrentPersonFrom = true, customLabel = null)
        assertTrue(vm.uiState.value.relations.first().isOutgoing)
    }

    @Test
    fun `incoming relation shows isOutgoing false`() {
        runBlocking {
            val bobId = fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet())
            fakeRelationRepository.addRelation(
                fromId = bobId,
                toId = aliceId,
                typeKey = RelationTypes.MENTOR_MENTEE.key,
            )
        }
        assertFalse(buildViewModel().uiState.value.relations.first().isOutgoing)
    }

    @Test
    fun `predefined relation label reflects perspective`() {
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet()) }
        val vm = buildViewModel()
        // Alice is the parent of Bob: isCurrentPersonFrom = true → from=Alice, to=Bob
        vm.addRelation(bobId, RelationTypes.PARENT_CHILD.key, isCurrentPersonFrom = true, customLabel = null)
        assertEquals("Parent", vm.uiState.value.relations.first().label)
    }

    @Test
    fun `predefined relation inverse label reflects perspective`() {
        runBlocking {
            val bobId = fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet())
            // Bob is parent of Alice: from=Bob, to=Alice
            fakeRelationRepository.addRelation(fromId = bobId, toId = aliceId, typeKey = RelationTypes.PARENT_CHILD.key)
        }
        // Alice's perspective: she is the child
        assertEquals("Child", buildViewModel().uiState.value.relations.first().label)
    }

    @Test
    fun `counterpartLabel for outgoing relation is the other person role`() {
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet()) }
        val vm = buildViewModel()
        // Alice is the parent: from=Alice, to=Bob → Alice's label="Parent", Bob's label="Child"
        vm.addRelation(bobId, RelationTypes.PARENT_CHILD.key, isCurrentPersonFrom = true, customLabel = null)
        assertEquals("Child", vm.uiState.value.relations.first().counterpartLabel)
    }

    @Test
    fun `counterpartLabel for incoming relation is the other person role`() {
        runBlocking {
            val bobId = fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet())
            // Bob is the parent: from=Bob, to=Alice → Alice's label="Child", Bob's label="Parent"
            fakeRelationRepository.addRelation(fromId = bobId, toId = aliceId, typeKey = RelationTypes.PARENT_CHILD.key)
        }
        assertEquals("Parent", buildViewModel().uiState.value.relations.first().counterpartLabel)
    }

    @Test
    fun `deleteRelation removes the relation`() {
        val bobId = runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet()) }
        val vm = buildViewModel()
        vm.addRelation(bobId, RelationTypes.FRIEND.key, isCurrentPersonFrom = true, customLabel = null)
        val relationId = vm.uiState.value.relations.first().relationId
        vm.deleteRelation(relationId)
        assertTrue(vm.uiState.value.relations.isEmpty())
    }

    @Test
    fun `allCategories is populated from category repository`() {
        runBlocking { fakeCategoryRepository.addCategory("Work") }
        val vm = buildViewModel()
        assertEquals(1, vm.uiState.value.allCategories.size)
        assertEquals("Work", vm.uiState.value.allCategories.first().name)
    }

    @Test
    fun `allCategories is empty when no categories exist`() {
        assertTrue(buildViewModel().uiState.value.allCategories.isEmpty())
    }

    @Test
    fun `allOtherAcquaintances excludes the current person`() {
        runBlocking { fakeAcquaintanceRepository.addAcquaintance("Bob", "", emptySet()) }
        val vm = buildViewModel()
        val ids = vm.uiState.value.allOtherAcquaintances.map { it.id }
        assertFalse(ids.contains(aliceId))
        assertEquals(1, ids.size)
    }
}
