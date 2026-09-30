# Tenter agent instructions

## Project Overview

Tenter is a JVM terminal-UI toolkit built on Mordant.

Callers describe prepared content, dispatch input intent, and inspect completed-frame observations;
Tenter owns glyph integrity, layout size, scroll following, panel geometry, and terminal-scope cleanup.

## Technology Stack

- Kotlin 2.4
- JVM 25
- Gradle 9.7.x (Kotlin DSL, convention plugins in `buildSrc/`)
- JUnit Jupiter + MockK + AssertJ for tests
- `explicitApi()` is on repo-wide — declare `public`/`internal` explicitly.

## Commands

```sh
# Build, tests, ABI validation, and the example's negative compilation check
./gradlew build

# Just testing
./gradlew test

# Compile and run the example against the packaged library jar and cached external dependencies
./gradlew :tenter-example:packagedSmoke

# Install for local consumers
./gradlew :tenter:publishToMavenLocal

# Rewrite the reference dump `tenter/api/tenter.api` from the current public API (`build` checks against it). Run only after intentionally accepting an API change
./gradlew :tenter:updateKotlinAbi
```

## Architecture

### Module Structure

Dependencies flow: `tenter-example` → `tenter`

- `tenter` - the library 
- `tenter-example` - external-consumer demonstration

### Tests

- Use focused tests for behavior changes; keep rendering helpers in Tenter's test sources, not its published artifact.

### Invariants

- The library is independent. Its main sources may depend only on Kotlin, kotlinx coroutines, and Mordant. Keep application policies in consumers.
- Preserve the package layering and external dependency checks in `tenter`'s architecture tests; see `docs/architecture.md`.
- Mordant types deliberately appear in the public API. Keep `mordant` and `kotlinx-coroutines-core` as `api` dependencies.
- Keep the example buildable as an ordinary module and as an isolated packaged-jar consumer.

## Tool Preferences

- **Use the LSP tool for code intelligence** (references, go-to-definition, hover, document/workspace symbols, call hierarchy, implementations): it resolves overloads, extension functions, and same-named symbols across modules that a text search conflates. Grep/Glob remain the right tool for strings, comments, and non-Kotlin files.
