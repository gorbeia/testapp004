# Acquaintance Tracker — Functional Spec

## Overview

Track people you know: group them by category, keep a bio, and record directed relations between them.

Accessible via the **People** tab in the bottom navigation bar.

---

## Screens

### People list (`AcquaintancesListScreen`)

* Shows all acquaintances as cards (name, bio preview, category chips — one per assigned category). Tapping a category chip navigates to the Categories screen.
* **Category filter row**: horizontal scrollable `FilterChip` row — "All" + one chip per existing category. Selecting a chip shows people who have that category **or any of its descendants** assigned.
* **FAB (+)**: navigates to Add Person screen.
* **AccountTree icon (top-right)**: navigates to the Browse Categories screen.
* **⋮ overflow menu**: "Manage categories" → Categories screen; "Check for updates" → update dialog.
* Category chips on person cards navigate to the Categories screen.
* Swipe-delete is not implemented; a delete icon is shown on each card.
* Empty state: "No people yet" / "No people in this category".

### Browse Categories (`CategoryBrowseScreen`)

* Purpose: explore the category tree to locate a category and jump to its canvas. Designed for corpora with dozens of categories.
* **Top bar**: "Browse Categories"; back button.
* Root categories are shown alphabetically. Categories with children display a **chevron (›)** that rotates to **∨** when expanded.
* Tapping a row with children **toggles** it expanded/collapsed. Only the visible subtree is shown at each moment (lazy expansion).
* Expanded children are also listed alphabetically and indented 24 dp per depth level.
* Each row — at any depth — has a **Hub icon** on the right; tapping it navigates directly to `CategoryCanvasScreen` for that category.
* Leaf categories (no children) show a fixed 24 dp spacer instead of the chevron.
* Empty state: "No categories yet".

### Person detail (`AcquaintanceDetailScreen`)

* **Top bar**: person's name; back, edit (pencil), delete (bin) actions.
* **Bio card**: shows the bio text; or "No bio added" placeholder. All assigned categories are shown as chips above the bio. Tapping a category chip navigates to the Categories screen.
  - A pencil icon in the top-right of the card enters **inline edit mode**: the bio is replaced by a multi-line text field with Save and Cancel buttons. Saving persists immediately; Cancel restores the previous text. No navigation required.
* **Relations section**: list of directed relations.
  - Outgoing (this person → other): shows `→ label →` + tappable name of other person.
  - Incoming (other → this person): shows `← label ←` + tappable name of other person.
  - Delete icon on each relation card.
  - (+) icon next to "Relations" header opens the Add Relation dialog.
* Deleting the person navigates back automatically.
* Tapping another person's name in a relation navigates to their detail screen.

### Category canvas (`CategoryCanvasScreen`)

Spatial overview of the people in a category and their relationships.

**Entry points:**
* Hub icon on any category row in the Categories screen.
* Hub icon on any category row in the Browse Categories screen.

**Layout:**
* People are rendered as circular nodes labelled with their name.
* Directed relations between people in the category are drawn as arrowed lines; the label sits at the midpoint of the edge and is perspective-aware (uses `Relation.labelFor(fromId)`).
* Only relations where **both** endpoints belong to the category (or its descendants) are drawn; cross-category relations are invisible from the canvas.
* Nodes are grouped by connected component (Union-Find). Each component's nodes are arranged in a circle. Components are placed in a left-to-right grid (max 3 per row), sorted largest first.
* Isolated people (no intra-category relations) appear as single-node clusters.

**Interaction:**
* **Pinch / two-finger drag**: zoom in/out, centred on the pinch midpoint.
* **One-finger drag**: pan the canvas.
* **Tap a node**: navigates to that person's detail screen.
* **Initial fit-to-view**: on first render the canvas automatically zooms and pans so all nodes fit with padding (capped at 1.2× zoom).

**Scope:**
* Includes people from the selected category **and all its descendants** (same recursive logic as the list-view filter).

**FAB (+):** navigates to the Add Person screen with this category pre-checked in the category multi-select.

**States:**
* Loading: spinner while data is fetched.
* Empty: "No people in this category" when the category (and descendants) has no members.

### Add / Edit person (`AddEditAcquaintanceScreen`)

* **Top bar**: "New Person" or "Edit Person"; back button; "Save" action (disabled when name is blank).
* **Name field** (required).
* **Bio field** (optional, multi-line).
* **Categories multi-select** (optional): shown only when at least one category exists. Displayed as a card containing one checkbox row per category, indented by tree depth. A person can be assigned to any number of categories simultaneously.
* When opened from the category canvas FAB, the canvas's category is pre-checked.
* **On save (add mode)**: navigates to the new person's detail screen, giving immediate access to link a contact and add relations.
* **On save (edit mode)**: navigates back to the detail screen.

### Categories (`CategoriesScreen`)

* **Top bar**: "Categories"; back button.
* List of existing categories displayed in **tree order** with visual indentation (children indented under their parent, prefixed with `└`).
* Each item has three icons: **hub (canvas)**, **edit (pencil)**, **delete (bin)**.
  - Hub icon: navigates to the Category canvas for that category.
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
