# ADR-006: Hilt for Dependency Injection

**Date:** 2026-05-31
**Status:** Accepted

## Context

As the app grows beyond a single screen with in-memory state, ViewModels need to receive dependencies (repositories, use cases) rather than create them directly. Without a DI framework, wiring grows tedious and tests require manual constructor injection for every object graph.

## Decision

Use **Hilt** (Dagger-backed) as the dependency injection framework.

- Application class annotated `@HiltAndroidApp`
- Activities annotated `@AndroidEntryPoint`
- ViewModels annotated `@HiltViewModel` with `@Inject constructor`
- Bindings declared in `@Module @InstallIn(SingletonComponent::class)` objects under `di/`
- Navigation uses `hiltViewModel()` from `androidx.hilt:hilt-navigation-compose`
- Code generation via KSP (not KAPT)

## Consequences

**Good:**
- `@HiltViewModel` provides scoped ViewModel instances automatically
- Swapping implementations (e.g., real persistence for in-memory) requires changing only the module binding
- Unit tests bypass Hilt entirely — construct ViewModels with fake dependencies directly

**Bad:**
- Build times increase slightly due to code generation
- Errors in the DI graph surface at compile time, which is verbose but better than runtime crashes

## Alternatives considered

- **Manual DI** — Fine for a single ViewModel but doesn't scale; ruled out
- **Koin** — Simpler setup but runtime-only graph validation; Hilt's compile-time safety is worth the overhead
