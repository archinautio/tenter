# Examples and troubleshooting

## Run the showcase

The example application is a showcase of every Tenter widget. From the Tenter checkout, build
and run the installed launcher in a real terminal:

```sh
./gradlew :tenter-example:installDist
tenter-example/build/install/tenter-example/bin/tenter-example
```

On Windows use `gradlew.bat` and the launcher's `.bat` file. It is designed for a 100×30
terminal and stays usable at 80×24:

- Below 100 columns the help panel starts minimized.
- Below 26 rows the title gives up its row.

The page list on the left chooses what the middle panel shows. Each page explains itself, has
keys of its own, and keeps its state while you look at other pages. The page list, the page,
and the help are themselves a [panel layout](panels.md#create-a-panel-layout): move the focus,
step a panel through its modes, scroll, and click to see one at work.

| Page | What it shows | Guide |
| --- | --- | --- |
| Text | `Paragraph` wrapping, ellipsizing, and alignment at a width you change, styled `Title`s on the top and bottom borders, aligned `writeLine`, styled spans, `writeRow`, `VerticalText` | [Show text](layouts.md#show-text) |
| Composition | `Block` borders, glyphs, and padding, `Stack`, `Row` constraints, `Flow`, `Rect.aligned` | [Layouts](layouts.md#stack-content-vertically) |
| Selection | `SelectableRow` with a tri-state header, `Checkbox` in every state, checkbox glyphs | [Draw selectable rows](styling.md#draw-selectable-rows) |
| Scrolling | Two `Viewport`s with `Scrollbar`s: a list following `revealed()`, a log following its end | [Scroll a single widget](panels.md#scroll-a-single-widget) |
| Progress | `Gauge` styles, a timed task, `FlashMessage` in the status bar | [Widgets](styling.md#draw-selectable-rows) |
| Colors | The schemes `--scheme` accepts, every `StandardRole` and text attribute, fixed colors, a popup over `Clear` | [Apply colors](styling.md#apply-colors) |
| Animation | The "TENTER" marquee in block letters, its speed, and pausing it | [Animation](animation.md) |
| Keys | A `KeyMap` with a second context turned on ahead of the first | [Map keys](input.md#map-keys-to-application-actions) |

These keys work everywhere; the help panel lists the current page's keys as well:

| Input | Action |
| --- | --- |
| Tab / Shift-Tab | Focus the next or previous panel |
| [ / ] | Show the previous or next page |
| Up / Down | With the page list focused, choose a page; elsewhere, scroll the focused panel a row unless the page uses them |
| Enter | Focus the page chosen in the page list |
| + / - | Step the focused panel through its modes |
| Page Up / Page Down | Scroll the focused panel |
| Mouse wheel | Scroll the panel under the pointer |
| Left click | Focus a panel; choose a page or click inside it |
| q | Quit and restore the terminal |

For just the greeting:

```sh
tenter-example/build/install/tenter-example/bin/tenter-example --hello
```

### Choose a color scheme

The showcase draws with the scheme that matches what your terminal shows: `truecolor`,
`ansi256`, or `ansi16`, which it also uses when the terminal shows no color. Name one to
override that choice:

```sh
tenter-example/build/install/tenter-example/bin/tenter-example --scheme=ansi256
```

- An unknown name lists the valid ones, and the launcher exits with status 2.
- A scheme with more colors than the terminal shows prints a warning, then runs anyway.
- The status bar names the scheme in use, and whether the terminal or `--scheme` chose it.

## Noninteractive check

Compile and run the example against the packaged library:

```sh
./gradlew :tenter-example:packagedSmoke
```

It runs a headless check of every page at three terminal sizes.

Browse the [example sources](https://github.com/archinautio/tenter/tree/main/tenter-example/src/main/kotlin/tenterexample)
for the complete applications behind these guides. Everything under the `tenterexample`
package is application code, not library types to import into your own application.

## Dependency cannot be resolved

Check the local publication, repositories, and version against the
[installation instructions](index.md#install-locally).

## Input does not work under Gradle

Use `installDist` and launch the resulting executable from a real terminal.
The example intentionally rejects Gradle's `run` task because it does not provide
an interactive terminal. Avoid IDE output consoles for interactive checks.

## Glyphs look wrong

Check the [glyph choices](styling.md#choose-glyphs) and
[color scheme behavior](styling.md#apply-colors) against your terminal configuration.

## Content does not follow selection or resizing

Review [viewport state and reveal following](panels.md#scroll-a-single-widget) and
[resize handling](input.md#collect-events-and-repaint). The content must say what to follow:

- `SelectableRow` reveals its selected row by itself.
- Custom selectable content marks its target with `.revealed()`.
- A fixed-size drawing marks part of itself with `.revealed(area)`.
- A log follows its end with `.followingEnd()`.

## Leaving the terminal cleanly

Follow the [screen lifetime and input collection rules](input.md#keep-drawing-and-state-updates-together).

## JDK native-access warning

Mordant drives the terminal through one of two backends. It prefers the JDK's Foreign
Function & Memory (FFM) API, which runs only when the JVM grants native access. Without
that access it falls back to JNA, and the JVM prints a warning at startup:

```
WARNING: java.lang.System::load has been called by com.sun.jna.Native in an unnamed module
WARNING: Use --enable-native-access=ALL-UNNAMED to avoid a warning for callers in this module
```

The application still runs, but a future JDK will block that call. Grant native access so
Mordant uses FFM instead, the same way for every way you launch the application:

- **Gradle launcher** (`installDist`): add
  `applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")` to the
  `application` block and rebuild the launcher.
- **Executable jar** (`java -jar`): add the manifest attribute
  `Enable-Native-Access: ALL-UNNAMED`. The JVM reads it only for `-jar`, not with `-cp`.
- **IDE or a hand-written `java` command**: pass `--enable-native-access=ALL-UNNAMED` as a
  VM option.

If you merge dependencies into one jar, merge the files under `META-INF/services/` as
well. Mordant lists each backend in its own service file, and keeping only one of them
leaves Mordant on JNA even with native access granted.
