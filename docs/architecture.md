# Architecture

## Diagram

```
┌─────────────────────────────────────────────────────┐
│  UI Layer  (Jetpack Compose)                        │
│                                                     │
│  Composable screens observe UiState and call        │
│  ViewModel methods on user interaction.             │
│  No business logic lives here.                      │
└──────────────────┬──────────────────────────────────┘
                   │  StateFlow<UiState>  (read)
                   │  fun someAction()    (write)
                   ▼
┌─────────────────────────────────────────────────────┐
│  ViewModel Layer  (androidx.lifecycle.ViewModel)    │
│                                                     │
│  Holds all non-ephemeral state as StateFlow.        │
│  Contains all business logic and state mutations.   │
│  Survives configuration changes.                    │
└──────────────────┬──────────────────────────────────┘
                   │  reads / transforms
                   ▼
┌─────────────────────────────────────────────────────┐
│  Model Layer  (plain Kotlin data classes)           │
│                                                     │
│  No Android dependencies. Easily unit-testable.     │
└─────────────────────────────────────────────────────┘
```

See [ADR-002](decisions/ADR-002-architecture-pattern.md) for the rationale behind this pattern.

---

## Package structure

```
com.example.testapp004/
│
├── MainActivity.kt             # Single activity; hosts the Compose content tree
│
├── model/
│   └── Note.kt                 # Plain Kotlin data classes; no Android deps
│
├── viewmodel/
│   └── NotesViewModel.kt       # ViewModel + NotesUiState data class
│
├── navigation/
│   └── AppNavigation.kt        # NavHost + sealed Screen routes
│
└── ui/
    ├── screens/
    │   └── HomeScreen.kt       # One file per screen; composable + private sub-composables
    └── theme/
        ├── Color.kt
        ├── Type.kt
        └── Theme.kt
```

---

## Key patterns

### UiState

Every screen has a single `data class` that fully describes what it renders:

```kotlin
data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val isAddNoteDialogOpen: Boolean = false
)
```

State is exposed as `StateFlow` and mutated atomically:

```kotlin
private val _uiState = MutableStateFlow(NotesUiState())
val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

fun deleteNote(noteId: Long) {
    _uiState.update { state -> state.copy(notes = state.notes.filter { it.id != noteId }) }
}
```

### Screen composables

Each screen receives its ViewModel as a parameter (not created internally):

```kotlin
@Composable
fun HomeScreen(viewModel: NotesViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    // ...
}
```

### Navigation

Routes are a sealed class; the NavHost is the single source of truth for
screen composition:

```kotlin
sealed class Screen(val route: String) {
    object Home : Screen("home")
}

NavHost(navController, startDestination = Screen.Home.route) {
    composable(Screen.Home.route) { HomeScreen(viewModel = notesViewModel) }
}
```

---

## Module structure

Single `:app` module. Multi-module separation (e.g., `:feature:notes`, `:core:model`)
will be considered if the codebase grows to the point where build times or team
boundaries justify it. Any such decision will be recorded as an ADR.

---

## Testing strategy

| Test type | Location | Framework | What it covers |
|-----------|----------|-----------|----------------|
| Unit | `app/src/test/` | JUnit4 | ViewModel logic, business rules |
| UI | `app/src/androidTest/` | Compose test rules | Screen rendering, user flows |

ViewModels are tested by instantiating them directly — no DI framework or mocking
library is required at the current scope.
