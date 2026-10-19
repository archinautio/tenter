# Scrolling and panels

## Scroll a single widget

`Viewport` is a `StatefulWidget<ViewportState>`. Keep a `ViewportState` for the lifetime
of the scrollable area and render the viewport with it on every frame:
`frame.renderStatefulWidget(Viewport(content), area, state)`. To move down one row,
call `state.scrollBy(dx = 0, dy = 1)` and draw again. Negative deltas move up or left.
Give the viewport a `block` to frame it:
`Viewport(content, block = Block.bordered(title = "LOG"))` keeps the border fixed while
the content scrolls, and paints scrollbar thumbs on its right and bottom borders. To make
a border scroll away with the content instead, put it on the content:
`Viewport(content.block(Block.bordered()))`.

After rendering, `state.settled` describes the completed frame, including its scroll
offset. It is initially null. Keep the state between frames; recreating it resets
scrolling. A changed reveal target or either viewport dimension re-engages following;
unchanged dimensions and targets preserve manual scrolling. Use `requestRecenter()` to center the content's current reveal target on
the next drawable frame.

## Create a panel layout

Panels combine content with a title, border, and scrolling. Keep one `PanelSet` for
the screen's lifetime: it is the state of the `Panels` widget, which you render each
frame with your current application state.

This excerpt from the compiled [panel example](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleApp.kt)
creates a catalog and a help panel. `ExamplePanelId` identifies each panel;
`ExampleState` supplies the selected row and checked items. The example's
`helpContent` and `ExampleListView` produce the displayed content.

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/ExampleApp.kt:panels"
```

Each mode's builder returns the panel's content; the width belongs to the panel. Give
a panel a `width` for a fixed-width slot, or leave it null to share the space the layout
allocates. A minimized panel is `minimizedWidth` wide (`Panel.MINIMIZED_WIDTH` by
default), and a maximized one takes the whole area. Widths are outer widths, including
borders and padding.

Use `PanelSet.mainAndSides(rows, listOf(help))` for a main area with side panels: the
main panel declares no width and every side declares one. Use
`PanelSet.uniform(listOf(rows, help))` for proportional columns, where panels with a
width form a trailing fixed group. Both check widths when the set is built. Create fresh
panels for independent sets: a panel instance belongs to one set.

## Render and control panels

`ExampleApp.render(frame)` runs inside `terminal.draw`. It renders a `Panels` widget,
built from the current application state and the visible ids, into an area of the
frame with the set as its state:

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/ExampleApp.kt:panel-render"
```

`visible` contains the ids to display. Render into any `Rect`; split the frame with
`Layout` to leave room for a header or status line. Focus a declared id with `panels.focus(id)`.
Use `scroll(panels.focused, 0, 1)` to scroll the focused panel a row and
`page(panels.focused, 1)` to scroll it a page; pass another id, such as the panel under
the mouse, to scroll that one. `cycleFocusedMode(1)` cycles the focused panel through its
declared modes.
Repaint after changing state or issuing a command. Use ids declared by the set;
unknown ids throw `IllegalArgumentException`.

After rendering, read rectangles and scroll offsets from `panels.layout`, a
`PanelLayout` in terminal (frame) coordinates. It is null before the first frame. A slot with
no drawable frame can have a null scroll observation. Hidden panels retain their
state and can be shown again without recreating the set. Focus remains on a hidden
panel, so focused commands still target it. Without a displayed viewport, a page
command queues one row per direction. Recenter requests wait until the panel has
a drawable frame, including across hidden or zero-sized frames.

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

## Reserve space in uniform layouts

By default, visible proportional panels share the remaining width. Set
`reservedColumns` when hidden proportional panels should keep their slots. Visible
minimized proportional panels reclaim their reserved slot. Panels that declare a width
form a trailing group. An empty visible set is valid, and small screens clip panel areas.

## Recover from a failed render

If a panel fails to paint, the set keeps its previous completed layout and scroll
observations, along with queued scrolling and recenter requests for retry. When the
failure escapes `terminal.draw`, the terminal sends nothing and keeps the previous
frame as its diff base. Side effects in application callbacks are not rolled back.
Published layout lists are immutable observations.
