# Tenter agent instructions

Tenter is a JVM terminal-UI toolkit built on Mordant: the `tenter` library and `tenter-example`, an external-consumer demonstration that depends on it.

Kotlin 2.4, JVM 25, Gradle 9.7 (Kotlin DSL, convention plugins in `buildSrc/`), JUnit Jupiter + MockK + AssertJ. `explicitApi()` is on repo-wide, so declare `public`/`internal` explicitly.

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

## Where things are documented

- [docs/architecture.md](docs/architecture.md): dependency boundaries, package layering, and implementation invariants. Read it before changing library structure.
- [docs/site/](docs/site/index.md): every caller-facing contract. Update the guide that owns a behavior when changing it.
- [docs/documentation.md](docs/documentation.md): where each kind of documentation belongs, and how to validate the site.

## Design principles

- Single Responsibility: each module resolves one behavior.
- Prefer polymorphism (sealed types, strategy objects) over a boolean parameter that an `if` switches on.
- Lay content out with `Layout` and `Constraint` wherever possible.
- Drawing flows top-down; anything that travels up is returned data, never a side channel.
- Comment only what the code cannot say for itself.
- The API is pre-1.0 and exploratory: break it freely, with no compatibility layer.

## Tool Preferences

- **Use the LSP tool for code intelligence** (references, go-to-definition, hover, document/workspace symbols, call hierarchy, implementations): it resolves overloads, extension functions, and same-named symbols across modules that a text search conflates. Grep/Glob remain the right tool for strings, comments, and non-Kotlin files.
