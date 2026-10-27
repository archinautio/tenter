# Content and layouts

Tenter has two kinds of widget. A `Widget` paints into whatever area it is given, as in
Ratatui. A `MeasurableWidget` can also measure its height for a width before painting,
so it can be stacked, placed in columns, and scrolled.

## Show text

`Paragraph` shows lines of text, as Ratatui's `Paragraph` does. A `Line` is made of
`Span`s, each with its own `Style`, so one line can mix colors without computing columns:

```kotlin
val greeting = Paragraph("Hello from Tenter")
val status = Paragraph(
    listOf(
        Line("Connected", Style(StandardRole.SUCCESS)),
        Line(Span("key ", Style(StandardRole.INFO)), Span("description")),
    ),
    wrap = true,
)
```

A string is split into lines at each `\n`. Without `wrap`, each line takes one row and
is shortened with `…` to fit; with it, lines wrap at word boundaries.

## Write a custom widget

For content that is not plain text, implement `MeasurableWidget` and write it row by
row with a `RowWriter`; its height follows from what you wrote:

```kotlin
class Summary(private val items: List<String>) : MeasurableWidget {
    override fun write(writer: RowWriter) {
        items.forEach { writer.writeLine(it) }
    }
}
```

`writeLine` writes a `String` or a `Line` and advances; `newLine` leaves a blank row;
`write(column, text, row = …)` places text at a column, on the current row unless you
name an earlier one; `writer.width` is the available width. Place a child with
`writer.draw(child)`; never call a child's `write` yourself, since it would write into
your rows instead of its own. A child wider than `writer.width`, such as a large
`fixedSize` drawing, makes your widget that wide too, so a viewport can scroll to it; text
past the edge is clipped instead. `widget.measure(width)` returns the `Size` content takes.

## Stack content vertically

Use `Stack(listOf(first, second))` to place measurable widgets one below another. The example's
[help panel](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleApp.kt)
combines a `KeyHelp` with text showing the selected row and checked count.
A plain `Widget` cannot measure itself; to stack one, give it a size with
`widget.fixedSize(width, height)`.

## Arrange columns

Give each `Column` a width in terminal cells and a `MeasurableWidget`. This excerpt
from the compiled [list example](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleListView.kt)
places selection markers beside rows. `markerColumn` and `rowColumn` are measurable
widgets defined earlier in that file; the widths account for the available space.

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/ExampleListView.kt:columns"
```

`gutter` separates columns. Columns that wrap into another band have a blank row
between bands. Combine the resulting columns with other content using `Stack`.
Widths are display cells, so a wide character such as `中` occupies more than one.

## Add borders and padding

A `Block` holds borders, a title, and padding, as Ratatui's `Block` does. `Block()` has
no border and `Block.bordered(...)` draws all four sides; choose sides with `borders`,
such as `Borders.TOP` for a titled divider. A block without borders adds padding alone.

Surround measurable content with a block using `.block(...)`. The result is still
measurable. This excerpt frames a label in a bordered block with one cell of padding,
then measures it for an available width of eight cells:

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/ExampleMain.kt:padding"
```

See the [complete source and imports](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleMain.kt).
`markReveal(row = 0)` asks an enclosing viewport to keep the marked row visible; omit
it for a static label. In an allocated area, render the block and then render the
content into `block.inner(area)`:

```kotlin
val block = Block.bordered(title = "LOG")
frame.renderWidget(block, area)
frame.renderWidget(content, block.inner(area))
```

## Draw a frame

Each call to `terminal.draw { frame -> ... }` starts from a blank frame the size of the
terminal. Render widgets into rectangles of `frame.area`. When the block returns, Tenter
sends only the cells that changed since the previous frame. The
[getting started program](index.md#write-your-first-application-hello-tenter) shows these steps.
For content taller than the destination, add a [viewport or panels](panels.md).

Every widget receives a `Canvas` already clipped to its area, with `(0, 0)` at the
area's top-left corner, so it cannot paint over its neighbours. Implement `Widget` to
paint directly into a canvas, or `StatefulWidget<S>` when the widget needs state that
outlives a frame, such as a scroll position. Render the latter with
`frame.renderStatefulWidget(widget, area, state)`.

## Split the screen

`Layout` splits a rectangle into adjacent rectangles along one axis, as Ratatui's
`Layout` does. Each segment takes a `Constraint`:

```kotlin
val (header, body, footer) = Layout.vertical(
    Constraint.Length(1),
    Constraint.Fill(1),
    Constraint.Length(1),
).split(frame.area)
val (list, details) = Layout.horizontal(Constraint.Percentage(40), Constraint.Fill(1))
    .spacing(1)
    .split(body)
```

| Constraint | Size |
| --- | --- |
| `Length(n)` | exactly `n` cells |
| `Percentage(p)`, `Ratio(a, b)` | that share of the available cells, rounded down |
| `Min(n)` | at least `n`; grows into leftover space when there is no `Fill` |
| `Max(n)` | at most `n`; shrinks before any `Min` when space is short |
| `Fill(weight)` | leftover space, shared among fills by weight |

Leftover cells with no fill or min segment to take them stay empty after the last
segment. When the requests exceed the space, the last segments shrink first. Inside
a widget, split `canvas.area` and render children into `canvas.region(rect)`.

## Reuse content safely

Written text and placements are snapshots, but plain widgets given a size and
color-role objects are retained rather than deep-copied. Capture stable frame data in
those widgets when you need repeatable output. `fixedSize` uses the dimensions you
supply without painting to measure; it calls the original widget each time it paints.

A viewport paints only the rows and children inside its window, so painting costs
the visible area, not the content length. Preparation still records all logical
content, so very long content uses proportionally more memory and preparation time;
limit or page the application data you prepare when working with large datasets.
A plain widget given a size with `fixedSize` is painted in full once per frame wherever it is
placed, because its reveal request is only known after it paints.
