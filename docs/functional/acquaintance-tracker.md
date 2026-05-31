# Acquaintance Tracker — Functional Spec

## Overview

Track people you know: group them by category, keep a bio, and record directed relations between them.

Accessible via the **People** tab in the bottom navigation bar.

---

## Screens

### People list (`AcquaintancesListScreen`)

* Shows all acquaintances as cards (name, bio preview, category chip).
* **Category filter row**: horizontal scrollable `FilterChip` row — "All" + one chip per existing category. Selecting a chip shows only people in that category.
* **FAB (+)**: navigates to Add Person screen.
* **List icon (top-right)**: navigates to Categories screen.
* Swipe-delete is not implemented; a delete icon is shown on each card.
* Empty state: "No people yet" / "No people in this category".

### Person detail (`AcquaintanceDetailScreen`)

* **Top bar**: person's name; back, edit (pencil), delete (bin) actions.
* **Bio card**: shows the bio text; or "No bio added" placeholder. Category chip shown if a category is assigned.
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
* **Category dropdown** (optional): shown only when at least one category exists. Options include "None" + all categories.
* On save, navigates back.

### Categories (`CategoriesScreen`)

* **Top bar**: "Categories"; back button.
* List of existing categories with delete icon per item.
* **FAB (+)**: opens Add Category dialog (single-field text input).
* Empty state: "No categories yet".
* Deleting a category does NOT automatically clear it from existing people (the categoryId on those Acquaintances becomes orphaned; the category chip simply won't render).

### Add Relation dialog (inline in detail screen)

* Dropdown to select the target person (all other people in the system).
* Text field for the relation label (e.g. "works with", "mentors", "met at conference").
* "Add" button disabled until both a person and a non-blank label are selected.

---

## Data rules

* A person can have zero or more relations, both outgoing and incoming.
* Relation labels are free-form text; no validation beyond non-blank.
* Categories are independent entities; deleting a category does not cascade to people.
* All data is in-memory and lost on app restart (persistent storage is a future feature).
