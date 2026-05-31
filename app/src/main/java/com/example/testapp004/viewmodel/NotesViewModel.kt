package com.example.testapp004.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testapp004.data.NotesRepository
import com.example.testapp004.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val isAddNoteDialogOpen: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NotesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.notes.collect { notes ->
                _uiState.update { it.copy(notes = notes) }
            }
        }
    }

    fun addNote(title: String, content: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addNote(title.trim(), content.trim())
            _uiState.update { it.copy(isAddNoteDialogOpen = false) }
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
        }
    }

    fun openAddNoteDialog() {
        _uiState.update { it.copy(isAddNoteDialogOpen = true) }
    }

    fun closeAddNoteDialog() {
        _uiState.update { it.copy(isAddNoteDialogOpen = false) }
    }
}
