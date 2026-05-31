package com.example.testapp004.viewmodel

import androidx.lifecycle.ViewModel
import com.example.testapp004.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val isAddNoteDialogOpen: Boolean = false
)

class NotesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    fun addNote(title: String, content: String) {
        if (title.isBlank()) return
        val note = Note(
            id = System.currentTimeMillis(),
            title = title.trim(),
            content = content.trim()
        )
        _uiState.update { state ->
            state.copy(notes = state.notes + note, isAddNoteDialogOpen = false)
        }
    }

    fun deleteNote(noteId: Long) {
        _uiState.update { state ->
            state.copy(notes = state.notes.filter { it.id != noteId })
        }
    }

    fun openAddNoteDialog() {
        _uiState.update { it.copy(isAddNoteDialogOpen = true) }
    }

    fun closeAddNoteDialog() {
        _uiState.update { it.copy(isAddNoteDialogOpen = false) }
    }
}
