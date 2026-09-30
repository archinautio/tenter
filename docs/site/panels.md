# Scrolling and panels

## Scroll a single view

Keep a `ViewportState` for the lifetime of the scrollable area. For each frame,
draw `Viewport(content, state)` into its destination canvas. To move down one row,
call `state.scrollBy(dx = 0, dy = 1)` and draw again. Negative deltas move up or left.

After drawing, `state.settled` describes the completed frame, including its scroll
offset. It is initially null. Keep the state between frames; recreating it resets
scrolling. A changed reveal target or either viewport dimension re-engages following;
unchanged dimensions and targets preserve manual scrolling. Use `requestRecenter()` to center the content's current reveal target on
the next drawable frame.

## Create a panel layout

Panels combine content with a title, border, and scrolling. Keep one `PanelSet` for
the screen's lifetime and pass your current application state when rendering.

This excerpt from the compiled [panel example](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleApp.kt)
creates a catalog and a help panel. `ExamplePanelId` identifies each panel;
`ExampleState` supplies the selected row and checked items. The example's
`helpContent` and `ExampleListView` produce the displayed content.

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/ExampleApp.kt:panels"
```

Use `PanelSet.mainAndSides(rows, listOf(help))` for a main area with a side panel.
Use `PanelSet.uniform(listOf(rows, help), fixedWidthPanels = setOf(ExamplePanelId.HELP))`
for a uniform arrangement with a fixed-width help panel.

Choose `Presentation.allocated(content)` when the layout assigns the panel's width.
Use `Presentation.fixedWidth(content, width)` for side or minimized presentations;
the positive width includes borders and padding. Create fresh panels for independent
sets: a panel instance belongs to one set.

## Render and control panels

The example creates a buffer and passes its canvas and current state into the set.
This excerpt stores the completed layout for later inspection:

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/ExampleApp.kt:panel-render"
```

`visible` contains the ids to display. Focus a declared id with `panels.focus(id)`.
Use `scrollFocused(0, 1)` to scroll a row, `pageFocused(1)` to scroll a page, and
`cycleFocusedState(1)` to cycle through the panel's available presentations.
Repaint after changing state or issuing a command. Use ids declared by the set;
unknown ids throw `IllegalArgumentException`.

Read rectangles and scroll offsets from the returned `PanelLayout`. A slot with
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
minimized proportional panels reclaim their reserved slot. Fixed-width panels form
a trailing group. An empty visible set is valid, and small screens clip panel areas.

A fixed-width presentation's preference is ignored in an allocated slot. An
allocated presentation in a fixed-width slot is rejected; provide a fixed-width
presentation for that state instead.

## Recover from a failed render

If a panel fails to paint, the set keeps its previous completed layout and scroll
observations, along with queued scrolling and recenter requests for retry. Discard
the failed frame's canvas: its writes and any side effects in application callbacks
are not rolled back. Published layout lists are immutable observations.
