# Releasing

A release turns the `-SNAPSHOT` version in `gradle.properties` into a tagged version and
installs its artifacts in Maven Local. Nothing is pushed or uploaded.

## What the build produces

`:tenter` publishes through the Vanniktech Maven Publish plugin, which already meets Maven
Central's artifact requirements:

| Artifact | Source |
| --- | --- |
| `tenter-<version>.jar` | Compiled library |
| `tenter-<version>-sources.jar` | Kotlin sources |
| `tenter-<version>-javadoc.jar` | Dokka HTML of the public API |
| `tenter-<version>.pom` | Name, description, URL, license, developer, and SCM |
| `tenter-<version>.module` | Gradle module metadata |

Publications are signed only when signing keys are configured, either as the
`signingInMemoryKey` Gradle property or the standard `signing.keyId` properties. Without
them, a release still installs locally unsigned.

## Cut a release

1. Start on `main` with a clean working tree; the release refuses uncommitted or
   untracked files.
2. Run the release, answering the prompts for the release version and the next
   development version:

    ```sh
    ./gradlew release
    ```

    To accept the default versions without prompts, the current snapshot without
    `-SNAPSHOT` and then the next minor snapshot (`0.2.0`, then `0.3.0-SNAPSHOT`):

    ```sh
    ./gradlew release -Prelease.useAutomaticVersion=true
    ```

    To choose the versions without prompts, add `-Prelease.releaseVersion=<version>` and
    `-Prelease.newVersion=<version>-SNAPSHOT` to that command. Without `useAutomaticVersion`, the release
    version skips its prompt but the next version is still asked, with the given one as the
    default.

The release:

1. Checks the branch, the working tree, and that no dependency is a snapshot.
2. Removes `-SNAPSHOT` from `gradle.properties`.
3. Shows how `tenter/api/tenter.api` changed since the previous `v*` tag, before asking for
   the release version. See [API changes](#api-changes).
4. Fails if the version is lower than [semantic versioning](#api-changes) allows.
5. Shows the release version in the [getting-started guide](site/index.md), then runs
   `build` and `:tenter-example:packagedSmoke` on the release version. `build` includes
   the tests, the ABI check against `tenter/api/tenter.api`, the example's negative
   compilation check, and the docs version check.
6. Installs the release artifacts in `~/.m2/repository/io/archinaut/tenter/<version>/`.
7. Commits both files, and tags that commit `v<version>`.
8. Sets the next development version in both files and commits them. By default, that is
   the next minor version.

If a step fails, nothing is tagged. Restore `gradle.properties` and `docs/site/index.md`
with `git restore` before trying again.

## API changes

`build` already guarantees that `tenter/api/tenter.api` matches the compiled public API.
The release compares that dump with the one at the previous `v*` tag, shows the diff, and
fails when the release version is lower than [semantic versioning](https://semver.org/)
allows for the change:

| Dump change since the previous release | Lowest release before 1.0 | Lowest release from 1.0 |
| --- | --- | --- |
| Lines removed or changed: breaks callers | Next minor | Next major |
| Only lines added | Next patch | Next minor |
| None | Next patch | Next patch |

Since each release starts the next minor snapshot, a release before 1.0 passes unless its
version is lowered by hand. From 1.0, a breaking change needs
`-Prelease.releaseVersion=<next major>`, which the failure message suggests.

The check is a safety net, not a review. Some breaking changes only add lines, such as a new
abstract member in a public interface that callers implement. Read the diff before
confirming the version.

To see the report or run the check at any time:

```sh
./gradlew reportApiChanges checkSemanticVersion
```

## Docs version

`check` fails when the getting-started guide shows a version other than the one in
`gradle.properties`. After changing the version by hand, run:

```sh
./gradlew syncDocsVersion
```

## After the release

Push `main` and the tag when the release should become public:

```sh
git push --follow-tags origin main
```

The tag starts the [release workflow](../.github/workflows/release.yml), which:

1. Fails if the tag differs from `v` plus the version in `gradle.properties`.
2. Runs `build` and `:tenter-example:packagedSmoke` on the tagged commit.
3. Creates the GitHub release `Tenter <version>`, with the [notes](#release-notes) and the
   jar, sources jar, javadoc jar, POM, and module metadata attached.

## Release notes

[git-cliff](https://git-cliff.org/) writes the notes from the Conventional Commits between
the previous `v*` tag and this one, configured in [`cliff.toml`](../cliff.toml):

| Commits | Section |
| --- | --- |
| Any type with `!` or a `BREAKING CHANGE:` footer | Breaking changes, with the footer's text |
| `feat` | Features |
| `fix` | Bug fixes |
| `perf` | Performance |
| `build(deps)` | Dependency updates |
| `docs` | Documentation |
| Anything else, including the release's own `chore(release)` commits | Left out |

After those sections, the notes add:

- A **Public API** section: one sentence on how `tenter/api/tenter.api` changed since the
  previous release, and its diff folded in a `<details>` block. The first release has no
  earlier API to compare with.
- A **Full changelog** link comparing the two tags.

Write each commit subject as a line of the notes. Write a dependency bump as
`build(deps): bump Mordant to 3.1.1` when callers get that dependency: the `api`
dependencies and Kotlin. Bumps that only affect this repository's build, such as Gradle,
plugins, and test libraries, stay `build: bump …` and are left out.

With git-cliff installed (for example, `brew install git-cliff`), preview the notes of the
latest tag, or of the commits not released yet:

```sh
git cliff --latest --strip header
```

```sh
git cliff --unreleased --strip header
```

To write the Public API section as the workflow does:

```sh
./gradlew reportApiChanges -PapiNotes=build/api-notes.md
```
