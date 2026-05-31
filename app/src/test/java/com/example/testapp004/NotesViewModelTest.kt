package com.example.testapp004

import com.example.testapp004.util.MainDispatcherRule
import com.example.testapp004.viewmodel.NotesViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NotesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeNotesRepository
    private lateinit var viewModel: NotesViewModel

    @Before
    fun setup() {
        fakeRepository = FakeNotesRepository()
        viewModel = NotesViewModel(fakeRepository)
    }

    @Test
    fun `initial state has empty notes list`() {
        assertTrue(viewModel.uiState.value.notes.isEmpty())
    }

    @Test
    fun `initial state has dialog closed`() {
        assertFalse(viewModel.uiState.value.isAddNoteDialogOpen)
    }

    @Test
    fun `initial state is not loading and has no error`() {
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `addNote increases notes count`() {
        viewModel.addNote("Test Title", "Test Content")
        assertEquals(1, viewModel.uiState.value.notes.size)
    }

    @Test
    fun `addNote stores correct title and content`() {
        viewModel.addNote("My Title", "My Content")
        val note = viewModel.uiState.value.notes.first()
        assertEquals("My Title", note.title)
        assertEquals("My Content", note.content)
    }

    @Test
    fun `addNote trims whitespace from title and content`() {
        viewModel.addNote("  Trimmed  ", "  Content  ")
        val note = viewModel.uiState.value.notes.first()
        assertEquals("Trimmed", note.title)
        assertEquals("Content", note.content)
    }

    @Test
    fun `addNote with blank title does not add note`() {
        viewModel.addNote("   ", "Some content")
        assertTrue(viewModel.uiState.value.notes.isEmpty())
    }

    @Test
    fun `addNote closes dialog`() {
        viewModel.openAddNoteDialog()
        viewModel.addNote("Title", "Content")
        assertFalse(viewModel.uiState.value.isAddNoteDialogOpen)
    }

    @Test
    fun `deleteNote removes note from list`() {
        viewModel.addNote("Note 1", "Content 1")
        val noteId = viewModel.uiState.value.notes.first().id
        viewModel.deleteNote(noteId)
        assertTrue(viewModel.uiState.value.notes.isEmpty())
    }

    @Test
    fun `deleteNote only removes the targeted note`() {
        viewModel.addNote("Note 1", "")
        viewModel.addNote("Note 2", "")
        val firstId = viewModel.uiState.value.notes.first().id
        viewModel.deleteNote(firstId)
        assertEquals(1, viewModel.uiState.value.notes.size)
        assertEquals("Note 2", viewModel.uiState.value.notes.first().title)
    }

    @Test
    fun `notes are assigned unique ids`() {
        viewModel.addNote("Note 1", "")
        viewModel.addNote("Note 2", "")
        val ids = viewModel.uiState.value.notes.map { it.id }
        assertEquals(ids.distinct(), ids)
    }

    @Test
    fun `openAddNoteDialog sets isAddNoteDialogOpen to true`() {
        viewModel.openAddNoteDialog()
        assertTrue(viewModel.uiState.value.isAddNoteDialogOpen)
    }

    @Test
    fun `closeAddNoteDialog sets isAddNoteDialogOpen to false`() {
        viewModel.openAddNoteDialog()
        viewModel.closeAddNoteDialog()
        assertFalse(viewModel.uiState.value.isAddNoteDialogOpen)
    }
}
