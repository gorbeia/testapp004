package com.example.testapp004.data

import com.example.testapp004.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryNotesRepository @Inject constructor() : NotesRepository {
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    override val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    override suspend fun addNote(title: String, content: String) {
        val now = System.currentTimeMillis()
        _notes.update { it + Note(id = now, title = title, content = content, createdAt = now) }
    }

    override suspend fun deleteNote(noteId: Long) {
        _notes.update { list -> list.filter { it.id != noteId } }
    }
}
