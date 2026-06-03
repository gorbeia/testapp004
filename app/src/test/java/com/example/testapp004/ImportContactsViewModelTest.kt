package com.example.testapp004

import com.example.testapp004.model.ImportableContact
import com.example.testapp004.util.MainDispatcherRule
import com.example.testapp004.viewmodel.ImportContactsViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ImportContactsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeAcquaintanceRepository: FakeAcquaintanceRepository
    private lateinit var fakeCategoryRepository: FakeCategoryRepository
    private lateinit var fakeContactRepository: FakeContactRepository

    @Before
    fun setup() {
        fakeAcquaintanceRepository = FakeAcquaintanceRepository()
        fakeCategoryRepository = FakeCategoryRepository()
        fakeContactRepository = FakeContactRepository()
    }

    private fun buildViewModel() = ImportContactsViewModel(
        contactRepository = fakeContactRepository,
        acquaintanceRepository = fakeAcquaintanceRepository,
        categoryRepository = fakeCategoryRepository,
    )

    @Test
    fun `initial state has isLoading true`() {
        val vm = buildViewModel()
        assertTrue(vm.uiState.value.isLoading)
    }

    @Test
    fun `loadContacts populates contact list`() = runTest {
        fakeContactRepository.importableContacts = listOf(
            ImportableContact("key1", "Alice", "1990-05-15", "Engineer @ ACME", null),
            ImportableContact("key2", "Bob", null, null, null),
        )
        val vm = buildViewModel()
        vm.loadContacts()

        val contacts = vm.uiState.value.contacts
        assertEquals(2, contacts.size)
        assertEquals("Alice", contacts.find { it.lookupKey == "key1" }?.displayName)
        assertEquals("Bob", contacts.find { it.lookupKey == "key2" }?.displayName)
    }

    @Test
    fun `loadContacts marks already-linked contacts`() = runTest {
        fakeAcquaintanceRepository.addAcquaintance(
            name = "Alice",
            bio = "",
            categoryIds = emptySet(),
            androidContactLookupKey = "key1",
        )
        fakeContactRepository.importableContacts = listOf(
            ImportableContact("key1", "Alice", null, null, null),
            ImportableContact("key2", "Bob", null, null, null),
        )
        val vm = buildViewModel()
        vm.loadContacts()

        val contacts = vm.uiState.value.contacts
        assertTrue(contacts.find { it.lookupKey == "key1" }!!.isAlreadyLinked)
        assertFalse(contacts.find { it.lookupKey == "key2" }!!.isAlreadyLinked)
    }

    @Test
    fun `loadContacts populates birthday from contact`() = runTest {
        fakeContactRepository.importableContacts = listOf(
            ImportableContact("key1", "Alice", "1990-05-15", null, null),
        )
        val vm = buildViewModel()
        vm.loadContacts()

        assertEquals("1990-05-15", vm.uiState.value.contacts.first().birthday)
    }

    @Test
    fun `loadContacts builds bio from organization and notes`() = runTest {
        fakeContactRepository.importableContacts = listOf(
            ImportableContact("key1", "Alice", null, "Dev @ Corp", "Met at conference"),
        )
        val vm = buildViewModel()
        vm.loadContacts()

        assertEquals("Dev @ Corp\nMet at conference", vm.uiState.value.contacts.first().bio)
    }

    @Test
    fun `loadContacts builds bio with only organization when notes absent`() = runTest {
        fakeContactRepository.importableContacts = listOf(
            ImportableContact("key1", "Alice", null, "Dev @ Corp", null),
        )
        val vm = buildViewModel()
        vm.loadContacts()

        assertEquals("Dev @ Corp", vm.uiState.value.contacts.first().bio)
    }

    @Test
    fun `onContactToggled selects an unselected contact`() = runTest {
        fakeContactRepository.importableContacts = listOf(
            ImportableContact("key1", "Alice", null, null, null),
        )
        val vm = buildViewModel()
        vm.loadContacts()

        vm.onContactToggled("key1")
        assertTrue(vm.uiState.value.selectedLookupKeys.contains("key1"))
    }

    @Test
    fun `onContactToggled deselects an already-selected contact`() = runTest {
        fakeContactRepository.importableContacts = listOf(
            ImportableContact("key1", "Alice", null, null, null),
        )
        val vm = buildViewModel()
        vm.loadContacts()

        vm.onContactToggled("key1")
        vm.onContactToggled("key1")
        assertFalse(vm.uiState.value.selectedLookupKeys.contains("key1"))
    }

    @Test
    fun `onContactToggled ignores already-linked contacts`() = runTest {
        fakeAcquaintanceRepository.addAcquaintance(
            name = "Alice",
            bio = "",
            categoryIds = emptySet(),
            androidContactLookupKey = "key1",
        )
        fakeContactRepository.importableContacts = listOf(
            ImportableContact("key1", "Alice", null, null, null),
        )
        val vm = buildViewModel()
        vm.loadContacts()

        vm.onContactToggled("key1")
        assertFalse(vm.uiState.value.selectedLookupKeys.contains("key1"))
    }

    @Test
    fun `onSelectAll selects all non-linked contacts`() = runTest {
        fakeAcquaintanceRepository.addAcquaintance(
            name = "Alice",
            bio = "",
            categoryIds = emptySet(),
            androidContactLookupKey = "key1",
        )
        fakeContactRepository.importableContacts = listOf(
            ImportableContact("key1", "Alice", null, null, null),
            ImportableContact("key2", "Bob", null, null, null),
            ImportableContact("key3", "Charlie", null, null, null),
        )
        val vm = buildViewModel()
        vm.loadContacts()

        vm.onSelectAll()
        val selected = vm.uiState.value.selectedLookupKeys
        assertFalse(selected.contains("key1"))
        assertTrue(selected.contains("key2"))
        assertTrue(selected.contains("key3"))
    }

    @Test
    fun `onDeselectAll clears selection`() = runTest {
        fakeContactRepository.importableContacts = listOf(
            ImportableContact("key1", "Alice", null, null, null),
            ImportableContact("key2", "Bob", null, null, null),
        )
        val vm = buildViewModel()
        vm.loadContacts()

        vm.onSelectAll()
        vm.onDeselectAll()
        assertTrue(vm.uiState.value.selectedLookupKeys.isEmpty())
    }

    @Test
    fun `onCategoryToggled adds category`() {
        val vm = buildViewModel()
        vm.onCategoryToggled(42L)
        assertTrue(vm.uiState.value.selectedCategoryIds.contains(42L))
    }

    @Test
    fun `onCategoryToggled removes already-selected category`() {
        val vm = buildViewModel()
        vm.onCategoryToggled(42L)
        vm.onCategoryToggled(42L)
        assertFalse(vm.uiState.value.selectedCategoryIds.contains(42L))
    }

    @Test
    fun `importSelected creates acquaintances for each selected contact`() = runTest {
        fakeContactRepository.importableContacts = listOf(
            ImportableContact("key1", "Alice", "1990-05-15", null, null),
            ImportableContact("key2", "Bob", null, null, null),
        )
        val vm = buildViewModel()
        vm.loadContacts()

        vm.onContactToggled("key1")
        vm.onContactToggled("key2")
        vm.importSelected()

        assertEquals(2, fakeAcquaintanceRepository.acquaintances.value.size)
    }

    @Test
    fun `importSelected stores birthday on created acquaintance`() = runTest {
        fakeContactRepository.importableContacts = listOf(
            ImportableContact("key1", "Alice", "1990-05-15", null, null),
        )
        val vm = buildViewModel()
        vm.loadContacts()

        vm.onContactToggled("key1")
        vm.importSelected()

        val saved = fakeAcquaintanceRepository.acquaintances.value.first()
        assertEquals("1990-05-15", saved.birthday)
    }

    @Test
    fun `importSelected links contact lookup key to created acquaintance`() = runTest {
        fakeContactRepository.importableContacts = listOf(
            ImportableContact("key1", "Alice", null, null, null),
        )
        val vm = buildViewModel()
        vm.loadContacts()

        vm.onContactToggled("key1")
        vm.importSelected()

        val saved = fakeAcquaintanceRepository.acquaintances.value.first()
        assertEquals("key1", saved.androidContactLookupKey)
    }

    @Test
    fun `importSelected assigns selected categories to created acquaintance`() = runTest {
        val catId = fakeCategoryRepository.addCategory("Work")
        fakeContactRepository.importableContacts = listOf(
            ImportableContact("key1", "Alice", null, null, null),
        )
        val vm = buildViewModel()
        vm.loadContacts()

        vm.onContactToggled("key1")
        vm.onCategoryToggled(catId)
        vm.importSelected()

        val saved = fakeAcquaintanceRepository.acquaintances.value.first()
        assertTrue(saved.categoryIds.contains(catId))
    }

    @Test
    fun `importSelected with empty selection does nothing`() = runTest {
        fakeContactRepository.importableContacts = listOf(
            ImportableContact("key1", "Alice", null, null, null),
        )
        val vm = buildViewModel()
        vm.loadContacts()

        vm.importSelected()
        assertTrue(fakeAcquaintanceRepository.acquaintances.value.isEmpty())
        assertNull(vm.uiState.value.importedCount)
    }

    @Test
    fun `importSelected sets importedCount after success`() = runTest {
        fakeContactRepository.importableContacts = listOf(
            ImportableContact("key1", "Alice", null, null, null),
            ImportableContact("key2", "Bob", null, null, null),
        )
        val vm = buildViewModel()
        vm.loadContacts()

        vm.onSelectAll()
        vm.importSelected()

        assertEquals(2, vm.uiState.value.importedCount)
    }
}
