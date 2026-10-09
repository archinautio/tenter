# Content and layouts

Start with `contentWidget { cursor -> ... }` to write flowing text. Use `writeLine`
for a line and `newLine` for a blank line. A `ContentWidget` can measure itself for
the available width and can be composed with other content.

## Stack content vertically

Use `Stack(listOf(first, second))` to place content one below another. The example's
[help panel](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleApp.kt)
combines a `HelpWidget` with text showing the selected row and checked count.
A plain `Widget` only renders into an allocated area; if you need to compose one,
wrap it with `fixedContent(width, height, widget)` using its known dimensions.

## Arrange columns

Give each `Columns.Child` a width in terminal cells and a `ContentWidget`. This excerpt
from the compiled [list example](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleListView.kt)
places selection markers beside rows. `markerColumn` and `rowColumn` are content
widgets defined earlier in that file; the widths account for the available space.

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

The excerpt uses `Block`, `Padded`, `Padding`, and `contentWidget` from Tenter.
See the [complete source and imports](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleMain.kt).
`markRevealAt(0)` asks an enclosing viewport to keep the marked row visible; omit
it for a static label. Use the ordinary `Padded` widget and `Block` only when
rendering into an already allocated area. Like Ratatui's `Block`, a `Block` draws
only its border and title; render the content into `block.inner(area)`:

```kotlin
val block = Block(title = "LOG")
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

Recorded text and placements are snapshots, but raw widgets and color-role objects
are retained rather than deep-copied. Capture stable frame data in raw widgets when
you need repeatable output. `fixedContent` uses the dimensions you supply without
painting to measure; it calls the original widget each time it paints.

Prepared layouts retain all logical content for scrolling. Very long content uses
proportionally more memory, so limit or page the application data you prepare when
working with large datasets.
