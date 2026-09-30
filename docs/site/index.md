# Getting started

Tenter is a Kotlin/JVM toolkit for building terminal applications with text, widgets,
scrolling panels, keyboard and mouse input, and animation. It builds on Mordant.

!!! note "Development documentation"
    These guides follow `main`, currently `0.1.0-SNAPSHOT`. The API is pre-1.0 and may
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

In a separate Gradle application, use this `build.gradle.kts`:

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
    implementation("io.archinaut:tenter:0.1.0-SNAPSHOT")
}

kotlin { jvmToolchain(25) }
application { mainClass.set("MainKt") }
```

Add `rootProject.name = "hello-tenter"` to `settings.gradle.kts` and use a Gradle
9.7.1 wrapper (`gradle wrapper --gradle-version 9.7.1` with Gradle installed).
The dependency above resolves from your machine's Maven Local repository; it is
not an instruction to download Tenter from Maven Central.

## Write your first application

Save the following as `src/main/kotlin/Main.kt`. This is a complete program: it paints
a greeting and waits for `q`. The code is included from the compiled
[hello example](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/hello/HelloMain.kt),
with its example package declaration omitted.

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/hello/HelloMain.kt:6:"
```

Build a launcher and run it from a real terminal:

```sh
./gradlew installDist
build/install/hello-tenter/bin/hello-tenter
```

On Windows, use `gradlew.bat` and the generated `hello-tenter.bat` launcher.
See [input and lifecycle](input.md) for managing terminal cleanup and your application
event loop.

## Continue building

- [Content and layouts](layouts.md): compose text, columns, padding, and borders.
- [Scrolling and panels](panels.md): show more content than fits on screen.
- [Input and resizing](input.md): update your application in response to events.
- [Widgets and styling](styling.md): add selection, checkboxes, and colors.
- [Animation](animation.md): render frames over time.
- [Examples and troubleshooting](examples.md): run the full demonstration.
