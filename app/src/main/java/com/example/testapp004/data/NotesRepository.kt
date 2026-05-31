package com.example.testapp004.data

import com.example.testapp004.model.Note
import kotlinx.coroutines.flow.StateFlow

interface NotesRepository {
    val notes: StateFlow<List<Note>>

    suspend fun addNote(title: String, content: String)

    suspend fun deleteNote(noteId: Long)
}
