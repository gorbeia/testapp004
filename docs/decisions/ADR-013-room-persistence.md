# ADR-013: Room SQLite Persistence

**Date:** 2026-05-31
**Status:** Accepted

## Context

All app data (acquaintances, categories, directed relations) was stored in
`MutableStateFlow<List<T>>` singletons that lived only for the process lifetime.
Any data entered by the user was lost when the app was killed or the device
restarted. The repository interfaces and MVVM architecture were already designed
to allow swapping the storage layer without touching ViewModels or UI.

## Decision

Replace the three `InMemory*Repository` implementations with Room (SQLite) backed
`Room*Repository` implementations. No interfaces, ViewModels, or composables change.

**Schema — 4 tables:**

| Table | Primary key | Notable constraints |
|-------|-------------|---------------------|
| `acquaintances` | `id` autoincrement | — |
| `categories` | `id` autoincrement | `parent_id` FK → `categories.id` ON DELETE SET NULL |
| `relations` | `id` autoincrement | `from_id`, `to_id` FK → `acquaintances.id` ON DELETE CASCADE |
| `acquaintance_categories` | `(acquaintance_id, category_id)` composite | both columns FK CASCADE |

**Key design choices:**

- IDs use `@PrimaryKey(autoGenerate = true)` (replaces `System.currentTimeMillis()` which risked collisions on fast inserts)
- `Acquaintance.categoryIds: Set<Long>` is stored via the `acquaintance_categories` join table and repopulated using a Room `@Relation` query with `Junction`
- `ON DELETE SET NULL` on `categories.parent_id` replaces the manual parent-nulling logic that was in `InMemoryCategoryRepository`
- `ON DELETE CASCADE` on `relations.from_id/to_id` automatically cleans up relations when an acquaintance is deleted
- Repository `StateFlow` is backed by `dao.getAll().stateIn(applicationScope, SharingStarted.Eagerly, emptyList())`
- `addAcquaintance` and `updateAcquaintance` use `db.withTransaction { }` (room-ktx) to insert the entity and cross-refs atomically
- Domain model classes (`Acquaintance`, `Category`, `Relation`) remain plain Kotlin data classes; Room entity classes are separate with private mapper functions in each repository
- An `@ApplicationScope` `CoroutineScope` (provided by `DatabaseModule`) backs the `stateIn` calls, scoped to the process lifetime

## Alternatives Considered

| Option | Why rejected |
|--------|-------------|
| Annotate domain models as `@Entity` directly | Couples the domain layer to Room; `categoryIds: Set<Long>` cannot be a Room column without a type converter or restructuring |
| DataStore (key-value or Proto) | The data is relational with foreign keys and joins — DataStore is for flat preferences/settings |
| SQLite directly (no Room) | Room provides compile-time query verification, Flow integration, and migration support at no meaningful cost |
| Keep `System.currentTimeMillis()` IDs | Millisecond-granularity IDs can collide; `autoGenerate = true` is safer and semantically correct |

## Consequences

- App data survives process kill and device restart
- The schema is at version 1; future schema changes require `Migration` objects in `AppDatabase` (or `fallbackToDestructiveMigration` during development only)
- Unit tests are unaffected — they use `Fake*Repository` implementations and never touch Room
- `InMemoryAcquaintanceRepository`, `InMemoryCategoryRepository`, `InMemoryRelationRepository` are deleted; they are superseded by the Room implementations
