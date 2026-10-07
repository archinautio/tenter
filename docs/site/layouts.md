# Content and layouts

Start with `contentView { cursor -> ... }` to write flowing text. Use `writeLine`
for a line and `newLine` for a blank line. A `ContentView` can measure itself for
the available width and can be composed with other content.

## Stack content vertically

Use `Stack(listOf(first, second))` to place content one below another. The example's
[help panel](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleApp.kt)
combines a `HelpView` with text showing the selected row and checked count.
A plain `View` only paints into an allocated area; if you need to compose one,
wrap it with `fixedContent(width, height, view)` using its known dimensions.

## Arrange columns

Give each `Columns.Child` a width in terminal cells and a `ContentView`. This excerpt
from the compiled [list example](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleListView.kt)
places selection markers beside rows. `markerColumn` and `rowColumn` are content
views defined earlier in that file; the widths account for the available space.

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/ExampleListView.kt:columns"
```

`gutter` separates columns. Columns that wrap into another band have a blank row
between bands. Combine the resulting columns with other content using `Stack`.
Widths are display cells, so a wide character such as `中` occupies more than one.

## Add padding and borders

Use the `prepared` factories when padding or borders participate in a stack or
column layout. This excerpt prepares a bordered label with one cell of padding,
then measures it for an available width of eight cells:

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/ExampleMain.kt:padding"
```

The excerpt uses `Bordered`, `Padded`, `Insets`, and `contentView` from Tenter.
See the [complete source and imports](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleMain.kt).
`markRevealAt(0)` asks an enclosing viewport to keep the marked row visible; omit
it for a static label. Use the ordinary `Padded` and `Bordered` constructors only
when painting a raw view into an already allocated area.

## Draw a frame

Create a `ScreenBuffer` for the current terminal size, wrap it with `Canvas.of(buffer)`,
call your content's `draw` method, and send the buffer to the renderer. The
[getting started program](index.md#write-your-first-application-hello-tenter) shows these steps.
For content taller than the destination, add a [viewport or panels](panels.md).

## Reuse content safely

Recorded text and placements are snapshots, but raw views and color-role objects
are retained rather than deep-copied. Capture stable frame data in raw views when
you need repeatable output. `fixedContent` uses the dimensions you supply without
painting to measure; it calls the original view each time it paints.

Prepared layouts retain all logical content for scrolling. Very long content uses
proportionally more memory, so limit or page the application data you prepare when
working with large datasets.
