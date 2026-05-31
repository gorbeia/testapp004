package com.example.testapp004.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.example.testapp004.data.InMemoryNotesRepository
import com.example.testapp004.ui.theme.Testapp004Theme
import com.example.testapp004.viewmodel.NotesViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: NotesViewModel

    @Before
    fun setUp() {
        viewModel = NotesViewModel(InMemoryNotesRepository())
    }

    @Test
    fun emptyStateIsShownInitially() {
        composeTestRule.setContent {
            Testapp004Theme {
                HomeScreen(viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithText("No notes yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tap + to add your first note").assertIsDisplayed()
    }

    @Test
    fun fabOpensAddNoteDialog() {
        composeTestRule.setContent {
            Testapp004Theme {
                HomeScreen(viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithContentDescription("Add note").performClick()
        composeTestRule.onNodeWithText("New Note").assertIsDisplayed()
    }

    @Test
    fun addingNoteShowsItInList() {
        composeTestRule.setContent {
            Testapp004Theme {
                HomeScreen(viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithContentDescription("Add note").performClick()
        composeTestRule.onNodeWithText("Title").performTextInput("My Test Note")
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.onNodeWithText("My Test Note").assertIsDisplayed()
    }
}
