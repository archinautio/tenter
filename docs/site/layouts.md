# Content and layouts

Tenter has two kinds of widget. A `Widget` paints into whatever area it is given, as in
Ratatui. A `MeasurableWidget` can also measure its height for a width before painting,
so it can be stacked, placed side by side, and scrolled.

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

`align` places each row, using Mordant's `TextAlign`:

```kotlin
val heading = Paragraph("Match over", align = TextAlign.CENTER)
val body = Paragraph(report, wrap = true, align = TextAlign.JUSTIFY)
```

- `LEFT`, the default, and `NONE` start each row at the left edge.
- `CENTER` and `RIGHT` place each row, wrapped rows included, within the width. A centered
  row that cannot split its spare cells evenly puts the extra cell on its right.
- `JUSTIFY` widens the spaces between words until a wrapped row fills the width. The last row
  of each line stays flush left, so a paragraph without `wrap` is never justified.
- A line wider than the space is shortened with `…` at its end, whatever the alignment.

Alignment belongs to the paragraph, not to its lines. To center a heading above a
left-aligned body, stack two paragraphs. The showcase's **Text** page wraps, ellipsizes, and
aligns a paragraph at a width you change with the arrow keys.

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

A `RowWriter` offers:

- `writer.width`: the available width.
- `writeLine(text, align = …)`: writes a `String` or a `Line`, shortened to fit and placed
  by a `TextAlign` as a paragraph row is, and advances. `JUSTIFY` fills this one row.
- `writeRow(left, right)`: writes text flush left and flush right on one row, and advances.
- `write(column, text, row = …)`: places text at a column without advancing, on the current
  row unless you name an earlier one.
- `newLine()`: leaves a blank row.
- `draw(child)`: places a measurable child and advances past it. Never call a child's `write`
  yourself, since it would write into your rows instead of its own.
- `draw(widget, height)`: paints a plain `Widget` across the full width.

`write` and `writeLine` return the `Point` where the text ended, as Ratatui's
`Buffer::set_line` does, so the next piece can follow it: `writer.write(label.x + 1, value)`.

A child wider than `writer.width`, such as a large `fixedSize` drawing, makes your widget that
wide too, so a viewport can scroll to it. Text past the edge is clipped instead.

## Stack content vertically

Use `Stack(listOf(first, second))` to place measurable widgets one below another, with
`spacing` blank rows between them. Every page of the
[showcase](examples.md#run-the-showcase) is a `Stack` of headings and examples, and its
**Composition** page changes the spacing live.
A plain `Widget` cannot measure itself; to stack one, give it a size with
`widget.fixedSize(width, height)`.

## Place content side by side

`Row` places measurable widgets side by side with the same `Constraint`s as `Layout`, and
never wraps. This excerpt from the showcase's compiled [Composition page](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/pages/CompositionPage.kt)
places three boxed labels side by side: one 14 cells wide, one taking 30% of the width, and one
filling what is left. `box` frames a label in a bordered block.

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/pages/CompositionPage.kt:columns"
```

`spacing` separates the children. Widths are display cells, so a wide character such as
`中` occupies more than one. A detail pane beside a list is a row too:

```kotlin
val split = Row(
    listOf(
        Constraint.Length(24) to list,
        Constraint.Fill(1) to detail.block(Block(borders = Borders.LEFT, padding = Padding(left = 2))),
    ),
    spacing = 2,
)
```

Maximize the showcase's **Composition** page with `+` to watch its chips move between bands.

The row is as tall as its tallest child, and each child gets that full height, so a block's
border reaches the bottom of the row; a left border divides a child from its neighbour.
Composites never clip: a child measured wider than its share, such as a `fixedSize` drawing,
widens it and pushes the later children right, so a viewport can scroll to it. Only a
viewport clips.

## Flow content into bands

`Flow` lays children out at declared widths and wraps to a new band when the next one does
not fit, as text flows. Each band is a `Row`, and bands are stacked with a blank row between
them; a child wider than the available space keeps its width alone in its band:

```kotlin
val sheet = Flow(
    listOf(
        Constraint.Length(30) to summary,
        Constraint.Length(24) to equipment,
        Constraint.Length(40) to diagram,
    ),
    spacing = 2,
)
```

## Add borders and padding

A `Block` holds borders, a title, and padding, as Ratatui's `Block` does:

- `Block.bordered(...)` draws all four sides, and `Block()` none.
- `borders` chooses the sides, such as `Borders.TOP` for a titled divider.
- `borderType` chooses their glyphs, one of Mordant's `BorderType` sets.
- `title` takes a `Title`, described below.
- A block without borders adds padding alone.

A `Title` is the text on the top row, as a `String` or a styled `Line`. It can also carry a
badge, such as the key that focuses a panel, and an alignment:

```kotlin
val log = Block.bordered(title = Title("LOG", badge = "2"))
val match = Block.bordered(
    title = Title(Line(Span("MATCH "), Span("OVER", Style(bold = true))), align = TextAlign.CENTER),
    titleColor = StandardRole.ACCENT,
)
```

- `align` places the title and badge at the left, center, or right of the top row, clear of the
  corners.
- The block's `titleColor` fills every channel a span leaves unset, so a plain title follows a
  panel's focus color. A span that sets its own color keeps it, even on a focused panel.
- A title too wide for the row is shortened with `…`. When not even its first character fits,
  the badge shows alone, as on a minimized panel.

Surround measurable content with a block using `.block(...)`. The result is still
measurable: eight cells wide, it is five rows tall. This excerpt frames a label in a
bordered block with one cell of padding:

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/pages/CompositionPage.kt:padding"
```

See the [complete source and imports](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/pages/CompositionPage.kt), whose page
also cycles a block's borders, border glyphs, and padding.
`.revealed()` asks an enclosing viewport to keep the label visible; omit it for a static
label. In an allocated area, render the block and then render the
content into `block.inner(area)`:

```kotlin
val block = Block.bordered(title = Title("LOG"))
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

A widget paints with `canvas.set(x, y, symbol, style)` for one character and
`canvas.setString(x, y, text, style)` for text. Both apply the [style](styling.md) to what the
cell already shows, so a channel the style leaves unset keeps its painted value.

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

To place one rectangle of a known size inside an area, such as a popup, use `aligned`. It
centers by default. Pass Mordant's `TextAlign` and `VerticalAlign` to place it at an edge:

```kotlin
val popup = frame.area.aligned(Size(40, 7))
val toast = frame.area.aligned(Size(30, 1), TextAlign.RIGHT, VerticalAlign.BOTTOM)
```

A size larger than the area shrinks to it, so the result always lies inside.

## Reuse content safely

Written text and placements are snapshots, but plain widgets given a size and
color-role objects are retained rather than deep-copied. Capture stable frame data in
those widgets when you need repeatable output. `fixedSize` uses the dimensions you
supply without painting to measure; it calls the original widget each time it paints.

What costs what:

- **Painting** costs the visible area, not the content length: a viewport paints only the
  rows and children inside its window.
- **Preparation** records all logical content, so very long content uses proportionally more
  memory and time. Limit or page the application data you prepare for large datasets.
- **A `fixedSize` widget** is painted once per frame, straight into the viewport's buffer. It
  receives a canvas of its full size and writes outside the visible window are dropped, so a
  large drawing that is costly to paint should skip what it cannot show.

A `fixedSize` widget makes no visibility request of its own. Declare one where you give it
its size, such as `board.fixedSize(w, h).revealed(cursorArea)`.
