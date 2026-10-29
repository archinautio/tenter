# Architecture

Public API signatures are tracked by `tenter/api/tenter.api`.

## Package layering

`color`, `text`, and `input` are leaves. `screen` uses `color` and `text`; `widget` uses `input`, `color`, `screen`, and `text`; `animation` uses `screen`, `widget`, and `text`; `terminal` uses `input`, `color`, `screen`, and `widget`. `tenter/src/test/kotlin/io/archinaut/tenter/LayeringTest.kt` enforces the full allowed-dependency matrix.

`tenter/src/test/kotlin/io/archinaut/tenter/ArchitectureTest.kt` enforces the dependency boundaries below, prevents BattleTech imports, and keeps managed `Panel` mutations behind `PanelSet`.

The packages have distinct jobs: `color` owns semantic colors; `text` owns display-cell metrics; `screen` owns buffers, cells, styles, canvas painting, and `Layout`/`Constraint` rectangle splitting; `widget` owns the `Widget`, `MeasurableWidget`, and `StatefulWidget` contracts, the internal recorded layouts behind measurable widgets, and every widget (blocks, viewports, rows, gauges), with stateful panel composition (the `Panels` widget over its `PanelSet` state) in `widget.panel`; `animation` owns finite frame descriptions and elapsed-time playback; `input` owns key maps and their help hints; `terminal` owns the real and headless `Terminal`s, the `Screen` their scope opens, its per-draw `Frame`, diff rendering, terminal event flows, and screen lifecycle.

The rendering model follows Ratatui: `Screen.draw` hands a `Frame` to the caller, widgets render into a `Canvas` clipped to their `Rect`, and the screen presents the drawn `Buffer`. `Terminal` is sealed: each kind opens an internal `Session` per scope that presents frames. A real terminal's session diffs against its previous frame and renders styles with the scope's color scheme; a headless terminal's keeps each frame as a snapshot.

## Module and dependency boundaries

Dependencies flow from `tenter-example` (an external-consumer demonstration) to
`tenter` (the published library). Keep application policies in consumers. External
library dependencies are limited to Kotlin, kotlinx coroutines, and Mordant, in
addition to the JDK. Keep Mordant and coroutines as `api` dependencies because their
types are deliberately exposed in public interfaces. The example must remain
buildable both as an ordinary module and as an isolated packaged-jar consumer.

## Public behavior ownership

The public guides own caller-facing contracts. Consult them before changing the
corresponding implementation; do not maintain a second description here:

- [Content and layouts](site/layouts.md): measurable widgets, blocks, frames, layout splitting, snapshots, and memory costs.
- [Scrolling and panels](site/panels.md): state ownership, application-owned panel placement, completed-frame observations, hidden panels, and failed-render recovery.
- [Input and resizing](site/input.md): key-map validation, event collection, application confinement, and terminal lifetime.
- [Testing](site/testing.md): headless terminals, injected input, and frame assertions.
- [Widgets and styling](site/styling.md): glyph choices, display-cell sizing, and color scheme requirements.
- [Animation](site/animation.md): finite elapsed-time playback and caller scheduling.

## Implementation invariants

- Tenter is a library over Mordant, not a framework around it. Applications create a `Terminal`
  and decide when to enter its screen scope; Tenter creates a real Mordant terminal by default,
  and `Terminal(mordant)` accepts any configured one. Preserve direct Mordant types in
  `TerminalEvent.Input` and `KeyBinding`.
- The color scheme belongs to the `withScreen` scope, and only its `Screen` draws, on every kind of
  terminal. A headless terminal follows the same rules as a real one, without color, so consumers
  test frames and scripted input, not escape output. Keep test-only observation (`frame`, the
  input script) on `HeadlessTerminal`, not on `Terminal`.
- `KeyMap` copies context, layer, and group collections, and `PanelSet` copies its panel
  list. Do not expose mutable declaration collections.
- A panel's content builder runs once per rendered frame. Panels have no size: the
  application places each rendered panel in a rectangle every frame, typically split with
  `Layout` from its `PanelSet.modeOf`. Do not reintroduce panel widths, layout factories, or a
  library-chosen arrangement. Keep mode-specific options out of the common render operation.
- Drawing flows one way: sizes and visibility requests (`revealed`, `followingEnd`) travel up only
  as data in prepared layouts, through composites such as `Stack`, `Row`, `Flow`, and `.block()`;
  offsets travel down. Never route a request through the canvas or its buffer, and never let a
  plain widget's paint decide one. Composites never clip; only `Viewport` does.
- Keep scrolling responsibilities apart: `ViewportState` chooses where a frame scrolls to,
  `ScrollTarget` computes the offset, `Viewport` measures and paints, `Scrollbar` shows the
  settled position, and `Block` draws chrome only. A panel composes them and records the
  geometry it painted, so hit testing reads the completed frame instead of re-deriving insets.
- Each viewport settles as it paints; `PanelSet` publishes hit geometry once every panel has
  painted. Expose no provisional or second settled-state read path.
- Share complete grapheme segmentation and display-width rules across text, wrapping,
  truncation, styled spans, and widgets. Cover combining marks, variation selectors,
  ZWJ sequences, flags, keycaps, decomposed accents, CJK, and supplementary characters.
  Clip wide glyphs atomically without leaving continuation cells behind. Reject or
  sanitize invalid controls and isolated surrogates according to each operation's contract;
  validate dimensions and glyph inputs before painting.
- Resolve fixed colors in both foreground and background channels. Color scheme validation
  is eager; terminal color suppression must not alter glyphs or layout.
- Screen entry changes the alternate screen and cursor only for interactive terminals.
  Restore them on normal exit, entry failure, block failure, and cleanup failure.
  Suppress cleanup exceptions onto an existing failure. Input cancellation uses bounded polling,
  so stopping event collection for any reason releases raw mode promptly.
- Input vocabulary (actions, quit keys, wheel steps, key glyphs) belongs to the application;
  the key map is generic over the application's action type.

Internal chrome spacing is not a public API. Preserve the supported boundaries
instead of introducing a public transaction manager, application event-loop abstraction,
theme hot-swap mechanism, or deep-copy promise as an incidental implementation change.
