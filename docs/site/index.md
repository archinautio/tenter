# Getting started

Tenter is a Kotlin/JVM toolkit for building terminal applications with text, widgets,
scrolling panels, keyboard and mouse input, and animation. It builds on Mordant.

!!! note "Development documentation"
    These guides follow `main`, currently `0.2.0-SNAPSHOT`. The API is pre-2.0 and may
    change. Pin the version and review API changes before upgrading. Installation
    currently requires building the library locally.

## Install locally

Use **JDK 25** and **Kotlin 2.4.10**. Tenter is JVM-only. Its public API uses Mordant
3.0.2 and kotlinx-coroutines 1.11.0; both are included as transitive dependencies.

Clone the project and publish it to your local Maven repository:

```sh
git clone https://github.com/archinautio/tenter.git
cd tenter
./gradlew :tenter:publishToMavenLocal
```

## Write your first application "hello-tenter"

Create a separate directory with a Gradle application.

Add `rootProject.name = "hello-tenter"` to `settings.gradle.kts` and use this `build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm") version "2.4.10"
    application
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.archinaut:tenter:0.2.0-SNAPSHOT")
}

kotlin { jvmToolchain(25) }
application { mainClass.set("MainKt") }
```

The dependency above resolves from your machine's Maven Local repository.

### The main loop

Save the following as `src/main/kotlin/Main.kt`. This is a complete program: it draws
one frame with a greeting and waits for `q`. The code is included from the compiled
[hello example](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/hello/HelloMain.kt),
with its example package declaration omitted (use whichever package you want).

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/hello/HelloMain.kt:6:"
```

### Run the application

Build a launcher and run it from a real terminal:

```sh
./gradlew installDist
build/install/hello-tenter/bin/hello-tenter
```

On Windows, use `gradlew.bat` and the generated `hello-tenter.bat` launcher.
See [input and lifecycle](input.md) for managing terminal cleanup and your application
event loop.

## Core concepts

Tenter follows the shape of [Ratatui](https://ratatui.rs/concepts/). If you know
Ratatui, the names map directly:

| Tenter | Ratatui | Role |
| --- | --- | --- |
| `Terminal` | `Terminal` | Owns the screen buffers and sends only what changed on each `draw`. Wraps a Mordant terminal as its backend. |
| `Frame` | `Frame` | One frame inside `terminal.draw { frame -> }`: its `area` and `renderWidget` / `renderStatefulWidget`. |
| `Widget`, `StatefulWidget` | `Widget`, `StatefulWidget` | Something that renders into an area, optionally with caller-owned state. |
| `Canvas` | `Rect` + `Buffer` | The area a widget renders into: clipped, with local coordinates. |
| `Buffer`, `Cell`, `Style` | `Buffer`, `Cell`, `Style` | The grid of styled cells. |
| `Layout`, `Constraint` | `Layout`, `Constraint` | Split a `Rect` into smaller rectangles. |
| `Block`, `Borders`, `Padding` | `Block`, `Borders`, `Padding` | Borders with a title, and the space inside them. |
| `Paragraph`, `Line`, `Span` | `Paragraph`, `Line`, `Span` | Text: lines made of styled spans, shown by a paragraph widget. |

Tenter adds measurable widgets (`MeasurableWidget`) for content that measures its own
height, scrolling viewports, and managed panels on top of these concepts.

## Continue with:

- [Content and layouts](layouts.md): compose text, columns, padding, and borders.
- [Scrolling and panels](panels.md): show more content than fits on screen.
- [Input and resizing](input.md): update your application in response to events.
- [Widgets and styling](styling.md): add selection, checkboxes, and colors.
- [Animation](animation.md): render frames over time.
- [Examples and troubleshooting](examples.md): run the full demonstration.
