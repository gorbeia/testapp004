# Acquaintance Tracker — Functional Spec

## Overview

Track people you know: group them by category, keep a bio, and record directed relations between them.

Accessible via the **People** tab in the bottom navigation bar.

---

## Screens

### People list (`AcquaintancesListScreen`)

* Shows all acquaintances as cards (name, bio preview, category chips — one per assigned category).
* **Category filter row**: horizontal scrollable `FilterChip` row — "All" + one chip per existing category. Selecting a chip shows people who have that category **or any of its descendants** assigned.
* **FAB (+)**: navigates to Add Person screen.
* **List icon (top-right)**: navigates to Categories screen.
* Swipe-delete is not implemented; a delete icon is shown on each card.
* Empty state: "No people yet" / "No people in this category".

### Person detail (`AcquaintanceDetailScreen`)

* **Top bar**: person's name; back, edit (pencil), delete (bin) actions.
* **Bio card**: shows the bio text; or "No bio added" placeholder. All assigned categories are shown as chips above the bio.
* **Relations section**: list of directed relations.
  - Outgoing (this person → other): shows `→ label →` + tappable name of other person.
  - Incoming (other → this person): shows `← label ←` + tappable name of other person.
  - Delete icon on each relation card.
  - (+) icon next to "Relations" header opens the Add Relation dialog.
* Deleting the person navigates back automatically.
* Tapping another person's name in a relation navigates to their detail screen.

### Add / Edit person (`AddEditAcquaintanceScreen`)

* **Top bar**: "New Person" or "Edit Person"; back button; "Save" action (disabled when name is blank).
* **Name field** (required).
* **Bio field** (optional, multi-line).
* **Categories multi-select** (optional): shown only when at least one category exists. Displayed as a card containing one checkbox row per category, indented by tree depth. A person can be assigned to any number of categories simultaneously.
* On save, navigates back.

### Categories (`CategoriesScreen`)

* **Top bar**: "Categories"; back button.
* List of existing categories displayed in **tree order** with visual indentation (children indented under their parent, prefixed with `└`).
* Each item has an **edit (pencil)** icon and a **delete (bin)** icon.
  - Delete: deleting a parent **orphans** its children (their parent is cleared to `null`).
  - Edit: opens Edit Category dialog pre-filled with the current name and parent.
* **FAB (+)**: opens Add Category dialog.
  - Text field for the category name.
  - Optional parent-category dropdown (only shown when at least one category already exists); defaults to "None (top level)".
* **Edit Category dialog**: pre-filled name field + parent dropdown. The category being edited and all its descendants are excluded from the parent dropdown to prevent cycles. "Save" button disabled when name is blank.
* Empty state: "No categories yet".
* Deleting a category does NOT automatically clear it from existing people (the removed `categoryId` is silently dropped from rendered chips).

### Add Relation dialog (inline in detail screen)

* Dropdown to select the target person (all other people in the system).
* Text field for the relation label (e.g. "works with", "mentors", "met at conference").
* "Add" button disabled until both a person and a non-blank label are selected.

---

## Data rules

* A person can belong to **zero or more** categories simultaneously.
* Categories form a **tree**: each category has an optional `parentId`. Roots have `parentId = null`.
* Selecting a category filter shows people in that category **and all its descendants** (recursive).
* A person can have zero or more relations, both outgoing and incoming.
* Relation labels are free-form text; no validation beyond non-blank.
* Deleting a category orphans its children and does not cascade to people.
* All data is in-memory and lost on app restart (persistent storage is a future feature).
