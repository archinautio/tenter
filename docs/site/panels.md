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

Mark the content to follow with `.revealed()`, such as the selected item:
`writer.draw(if (selected) item.revealed() else item)`. When several requests are
made, the last one wins. For a log, create the state with
`ViewportState(stickToBottom = true)`, or pass `stickToBottom = true` to a `Panel`: the
viewport keeps the last row visible as content grows, stops when the user scrolls up, and
resumes once they scroll back to the bottom.

## Create a panel layout

Panels combine content with a title, border, and scrolling. Keep one `PanelSet` for
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

Each mode's builder returns the panel's content. A panel has no size: where it goes and
how big it is, in each mode, is your application's decision. Create the set with
`PanelSet(listOf(rows, details, checked, help))`; focus starts on the first panel. Create fresh panels for
independent sets: a panel instance belongs to one set.

## Render and control panels

`ExampleApp.render(frame)` runs inside `terminal.draw`. It splits a status bar off the top
of the frame, then renders a `Panels` widget, built from the current application state and a
rectangle for each panel to draw, with the set as its state:

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/ExampleApp.kt:panel-render"
```

Choose the rectangles every frame, usually by splitting the frame with `Layout` and picking
each panel's `Constraint` from `panels.modeOf(id)`. Only panels you give a rectangle are drawn,
so leaving an id out hides that panel. `MAXIMIZED` is a mode like the others: tenter tracks it
and keeps at most one panel maximized, but you decide what it looks like, typically by giving
that panel the whole area and leaving the others out. Rectangles are frame coordinates and are
clipped to the area the widget renders into; where two overlap, the one later in the map paints
over the earlier and receives hits. This example lets the catalog fill the width the
right-hand panels leave, each at its mode's width, and gives a maximized panel the whole area:

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/ExampleApp.kt:panel-layout"
```

Focus a declared id with `panels.focus(id)`.
Use `scroll(panels.focused, 0, 1)` to scroll the focused panel a row and
`page(panels.focused, 1)` to scroll it a page; pass another id, such as the panel under
the mouse, to scroll that one. `cycleFocusedMode(1)` cycles the focused panel through its
declared modes.
Repaint after changing state or issuing a command. Use ids declared by the set;
unknown ids throw `IllegalArgumentException`.

After rendering, read rectangles and scroll offsets from `panels.layout`, a
`PanelLayout` in terminal (frame) coordinates. It is null before the first frame;
`slotOf(id)` finds one drawn panel's slot. A slot with
no drawable frame can have a null scroll observation. Hidden panels retain their
state and can be shown again without recreating the set. When the focused panel is not
drawn, focus moves to the first drawn panel in declaration order; a frame that draws no
panel keeps the focus, so focused commands still target the hidden panel. Without a displayed viewport, a page
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

## Recover from a failed render

If a panel fails to paint, the set keeps its previous completed layout and scroll
observations, along with queued scrolling and recenter requests for retry. When the
failure escapes `terminal.draw`, the terminal sends nothing and keeps the previous
frame as its diff base. Side effects in application callbacks are not rolled back.
Published layout lists are immutable observations.
