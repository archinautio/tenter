# Scrolling and panels

## Scroll a single widget

`Viewport` is a `StatefulWidget<ViewportState>`. Keep a `ViewportState` for the lifetime
of the scrollable area and render the viewport with it on every frame:
`frame.renderStatefulWidget(Viewport(content), area, state)`. To move down one row,
call `state.scrollBy(dx = 0, dy = 1)` and draw again. Negative deltas move up or left.

A viewport draws no chrome. As in Ratatui, compose the frame yourself: render a `Block`, the
viewport into `block.inner(area)`, and then a `Scrollbar` over the border beside it, from the
settled state:

```kotlin
val block = Block.bordered(title = "LOG")
frame.renderWidget(block, area)
val inner = block.inner(area)
frame.renderStatefulWidget(Viewport(content), inner, state)
state.settled?.let { scroll ->
    val track = Rect(area.x + area.width - 1, inner.y, 1, inner.height)
    frame.renderStatefulWidget(Scrollbar.vertical(), track, scroll)
}
```

The border stays fixed while the content scrolls. To make a border scroll away with the
content instead, put it on the content: `Viewport(content.block(Block.bordered()))`.

After rendering, `state.settled` describes the completed frame, including its scroll
offset. It is initially null. Keep the state between frames; recreating it resets
scrolling.

The content says what it wants kept visible; the viewport decides when to follow it:

- `.revealed()` asks to keep a widget visible, such as the selected item:
  `writer.draw(if (selected) item.revealed() else item)`.
- `.revealed(area)` asks for part of it, such as the cursor in a `fixedSize` map.
- A revealed target is followed again when it moves or the viewport resizes. Otherwise manual
  scrolling stays where the user left it.
- `.followingEnd()` keeps a log's last row visible as it grows. Scrolling up stops it, and
  scrolling back to the end resumes it.

When several requests are made, the last one wins. Use `requestRecenter()` to center the
content's current request on the next drawable frame.

## Create a panel layout

Panels combine content with a title, border, scrollbars, and scrolling. Keep one `PanelSet` for
the screen's lifetime: it is the state of the `Panels` widget, which you render each
frame with your current application state.

This excerpt from the compiled [panel example](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleApp.kt)
creates its four panels: the catalog, which declares no minimized content and so can only
be maximized, the details and checked panels, which declare all three modes, and help.
`ExamplePanelId` identifies each panel; `ExampleState` supplies the selected row and checked
items. The example's `ExampleListView` and content functions produce the displayed content.

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/ExampleApp.kt:panels"
```

- Each mode's builder returns the panel's content, and runs once per rendered frame.
- A panel has no size: where it goes and how big it is, in each mode, is your application's
  decision.
- Create the set with `PanelSet(listOf(rows, details, checked, help))`. Focus starts on the
  first panel.
- A panel instance belongs to one set, so create fresh panels for independent sets.

## Render and control panels

`ExampleApp.render(frame)` runs inside `terminal.draw`. It splits a status bar off the top
of the frame, then renders a `Panels` widget, built from the current application state and a
rectangle for each panel to draw, with the set as its state:

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/ExampleApp.kt:panel-render"
```

Choose the rectangles every frame, usually by splitting the frame with `Layout` and picking
each panel's `Constraint` from `panels.modeOf(id)`:

- Only panels you give a rectangle are drawn, so leaving an id out hides that panel.
- `MAXIMIZED` is a mode like the others. Tenter keeps at most one panel maximized, but you
  decide what it looks like, typically by giving that panel the whole area and leaving the
  others out.
- Rectangles are frame coordinates, clipped to the area the widget renders into.
- Where two rectangles overlap, the one later in the map paints over the earlier and receives
  hits.

This example lets the catalog fill the width the right-hand panels leave, each at its mode's
width, and gives a maximized panel the whole area:

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/ExampleApp.kt:panel-layout"
```

Control the panels through the set, then repaint:

- `focus(id)` focuses a panel.
- `scroll(id, 0, 1)` scrolls a panel a row, and `page(id, 1)` a page. Pass `panels.focused`
  for the focused panel, or another id, such as the panel under the mouse.
- `cycleFocusedMode(1)` cycles the focused panel through its declared modes.
- `requestRecenter(id)` centers a panel's visibility request on its next drawable frame.

Use ids declared by the set; unknown ids throw `IllegalArgumentException`.

After rendering, `panels.layout` is a `PanelLayout` in terminal (frame) coordinates, null
before the first frame. `slotOf(id)` finds one drawn panel's slot:

| Field | What it holds |
| --- | --- |
| `outer` | The whole panel rectangle, border included |
| `content` | The part the scrolling viewport showed |
| `scroll` | The viewport's settled `ScrollState` |
| `contentBounds` | Where the content lay, extending past `content` where scrolled out of view |

A slot with no drawable frame has no `scroll` and no `contentBounds`.

Hidden panels keep their state, and come back as they were:

- When the focused panel is not drawn, focus moves to the first drawn panel in declaration
  order. A frame that draws no panel keeps the focus, so focused commands still target it.
- Without a displayed viewport, a page command queues one row per direction.
- Recenter requests wait until the panel has a drawable frame.

## Handle clicks and the mouse wheel

Call `hitTest(x, y)` with terminal coordinates after rendering. A hit identifies the
panel and, when applicable, the point in its content. Use that point rather than
subtracting border widths or scroll offsets yourself.

This excerpt handles wheel scrolling and toggles a clicked catalog row. `event`
is a Mordant `MouseEvent`, and `state` belongs to the example application:

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/ExampleApp.kt:mouse"
```

Clicks outside panels return null; clicks on panel chrome can have no content point.
See [input and resizing](input.md) for collecting events and repainting.

## Recover from a failed render

If a panel fails to paint:

- The set keeps its previous completed layout, so hit tests and paging still read the last
  frame that finished.
- Panels painted before the failure keep the offsets they settled on.
- The failed panel and those after it keep their queued scrolling and recenter requests for
  the next frame.
- When the failure escapes `terminal.draw`, the terminal sends nothing and keeps the previous
  frame as its diff base.
- Side effects in your own callbacks are not rolled back.
