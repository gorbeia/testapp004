package com.example.testapp004

import com.example.testapp004.data.NotesRepository
import com.example.testapp004.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeNotesRepository : NotesRepository {
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    override val notes: StateFlow<List<Note>> = _notes.asStateFlow()
    private var nextId = 1000L

    override suspend fun addNote(title: String, content: String) {
        val id = nextId++
        _notes.update { it + Note(id = id, title = title, content = content, createdAt = id) }
    }

    override suspend fun deleteNote(noteId: Long) {
        _notes.update { list -> list.filter { it.id != noteId } }
    }
}
