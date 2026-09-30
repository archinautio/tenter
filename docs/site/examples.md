# Examples and troubleshooting

## Run the demonstration

From the Tenter checkout, build and run the installed launcher in a real terminal:

```sh
./gradlew :tenter-example:installDist
tenter-example/build/install/tenter-example/bin/tenter-example
```

On Windows use `gradlew.bat` and the launcher's `.bat` file.
The catalog demonstrates scrolling through 600 rows, selection, checkboxes, a help
panel, mouse handling, and resizing.

| Input | Action |
| --- | --- |
| Up / Down | Move the selected row |
| Space | Toggle the selected row |
| h | Focus the help panel |
| m | Cycle the help panel's size |
| Page Up / Page Down | Scroll the focused panel |
| Mouse wheel | Scroll the panel under the pointer |
| Left click | Select and toggle a catalog row |
| q | Quit and restore the terminal |

For just the greeting:

```sh
tenter-example/build/install/tenter-example/bin/tenter-example --hello
```

## Noninteractive check

Compile and run the example against the packaged library:

```sh
./gradlew :tenter-example:packagedSmoke
```

Browse the [example sources](https://github.com/archinautio/tenter/tree/main/tenter-example/src/main/kotlin/tenterexample)
for the complete applications behind these guides. Types prefixed with `Example`
are application code, not library types to import into your own application.

## Dependency cannot be resolved

Check the local publication, repositories, and version against the
[installation instructions](index.md#install-locally).

## Input does not work under Gradle

Use `installDist` and launch the resulting executable from a real terminal.
The example intentionally rejects Gradle's `run` task because it does not provide
an interactive terminal. Avoid IDE output consoles for interactive checks.

## Glyphs look wrong

Check the [glyph choices](styling.md#choose-glyphs) and
[palette behavior](styling.md#apply-colors) against your terminal configuration.

## Content does not follow selection or resizing

Review [viewport state and reveal following](panels.md#scroll-a-single-view) and
[resize handling](input.md#collect-events-and-repaint). `SelectableRow` marks its
selected row for reveal; custom selectable content must mark its own target.

## Leaving the terminal cleanly

Follow the [screen lifetime and input collection rules](input.md#keep-drawing-and-state-updates-together).

## JDK native-access warning

On JDK 25, the terminal backend may print a warning about `System.load` and JNA.
The greeting still runs. If you choose to enable native access for that backend,
add `applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")`
to the consumer's `application` block and rebuild its launcher.
