package com.example.testapp004.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testapp004.data.AcquaintanceRepository
import com.example.testapp004.data.CategoryRepository
import com.example.testapp004.data.ContactRepository
import com.example.testapp004.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImportContactItem(
    val lookupKey: String,
    val displayName: String,
    val birthday: String?,
    val bio: String,
    val isAlreadyLinked: Boolean,
)

data class ImportContactsUiState(
    val contacts: List<ImportContactItem> = emptyList(),
    val selectedLookupKeys: Set<String> = emptySet(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val importedCount: Int? = null,
)

@HiltViewModel
class ImportContactsViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val acquaintanceRepository: AcquaintanceRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ImportContactsUiState(isLoading = true))
    val uiState: StateFlow<ImportContactsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.categories.collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
    }

    fun loadContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val importable = contactRepository.fetchImportableContacts()
                val existingKeys = acquaintanceRepository.acquaintances.value
                    .mapNotNull { it.androidContactLookupKey }.toSet()
                val items = importable.map { contact ->
                    ImportContactItem(
                        lookupKey = contact.lookupKey,
                        displayName = contact.displayName,
                        birthday = contact.birthday,
                        bio = buildBio(contact.organizationInfo, contact.notes),
                        isAlreadyLinked = contact.lookupKey in existingKeys,
                    )
                }
                _uiState.update { it.copy(contacts = items, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load contacts") }
            }
        }
    }

    fun onContactToggled(lookupKey: String) {
        _uiState.update { state ->
            if (state.contacts.find { it.lookupKey == lookupKey }?.isAlreadyLinked == true) return@update state
            val newKeys = if (lookupKey in state.selectedLookupKeys) {
                state.selectedLookupKeys - lookupKey
            } else {
                state.selectedLookupKeys + lookupKey
            }
            state.copy(selectedLookupKeys = newKeys)
        }
    }

    fun onSelectAll() {
        _uiState.update { state ->
            val selectable = state.contacts.filter { !it.isAlreadyLinked }.map { it.lookupKey }.toSet()
            state.copy(selectedLookupKeys = selectable)
        }
    }

    fun onDeselectAll() {
        _uiState.update { it.copy(selectedLookupKeys = emptySet()) }
    }

    fun onCategoryToggled(categoryId: Long) {
        _uiState.update { state ->
            val newIds = if (categoryId in state.selectedCategoryIds) {
                state.selectedCategoryIds - categoryId
            } else {
                state.selectedCategoryIds + categoryId
            }
            state.copy(selectedCategoryIds = newIds)
        }
    }

    fun importSelected() {
        val state = _uiState.value
        if (state.selectedLookupKeys.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val toImport = state.contacts.filter { it.lookupKey in state.selectedLookupKeys }
            for (contact in toImport) {
                acquaintanceRepository.addAcquaintance(
                    name = contact.displayName,
                    bio = contact.bio,
                    categoryIds = state.selectedCategoryIds,
                    birthday = contact.birthday,
                    androidContactLookupKey = contact.lookupKey,
                )
            }
            _uiState.update { it.copy(isLoading = false, importedCount = toImport.size) }
        }
    }

    private fun buildBio(organizationInfo: String?, notes: String?): String =
        listOfNotNull(organizationInfo, notes).joinToString("\n")
}
