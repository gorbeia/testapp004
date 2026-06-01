# ADR-020: Create related person directly from the relation dialog

**Date:** 2026-06-01
**Status:** Accepted

## Context

When adding a relation from a person's detail screen the user must pick an existing person from
a dropdown. If the target person has not been created yet, the user must leave the detail screen,
create the person, return, and then add the relation — a multi-step interruption.

## Decision

Add a "Create new person" button to the AddRelationDialog. Clicking it:

1. Closes the dialog.
2. Navigates to AddEditAcquaintanceScreen with a new `returnToPersonId` route argument that
   identifies the originating person's detail screen.
3. After the new person is saved, instead of navigating to the new person's detail screen,
   `navController.popBackStack()` is called and the new person's ID is written into the
   originating back-stack entry's `savedStateHandle` under the key `pendingRelationWithId`.
4. The detail screen's composable route observes that key via a `LaunchedEffect`; when it fires,
   it calls `viewModel.openAddRelationDialogWithPreselectedPerson(newId)`.
5. The AddRelationDialog reopens with the newly created person already selected in the
   person picker, so the user only needs to choose the relation type and confirm.

The `pendingNewRelationPersonId` field is cleared from `AcquaintanceDetailUiState` whenever the
dialog is closed, preventing stale pre-selection on subsequent manual opens.

## Alternatives Considered

| Option | Why rejected |
|--------|-------------|
| Inline name field in dialog | Mixing person creation into the relation dialog makes both flows more complex and violates single-responsibility |
| Navigate to new person's detail then back | Loses the relation-creation context and requires the user to re-open the dialog manually |
| Route argument on AcquaintanceDetail for pending person | Duplicating data already available via `savedStateHandle`; unnecessarily broadens the route surface |

## Consequences

- `Screen.AddEditAcquaintance` gains a `returnToPersonId` optional argument (default -1).
- `AcquaintanceDetailScreen` gains an `onCreateNewPersonClick` callback parameter.
- `AcquaintanceDetailUiState` gains `pendingNewRelationPersonId: Long?`.
- `AcquaintanceDetailViewModel` gains `openAddRelationDialogWithPreselectedPerson(Long)`.
- The normal "add person" flow (no `returnToPersonId`) is unchanged.
