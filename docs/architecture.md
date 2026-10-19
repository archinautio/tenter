# Architecture

Public API signatures are tracked by `tenter/api/tenter.api`.

## Package layering

`palette`, `text`, and `input` are leaves. `screen` uses `palette` and `text`; `widget` uses `input`, `palette`, `screen`, and `text`; `animation` uses `screen`, `widget`, and `text`; `terminal` uses `input`, `palette`, `screen`, and `widget`. `tenter/src/test/kotlin/io/archinaut/tenter/LayeringTest.kt` enforces the full allowed-dependency matrix.

`tenter/src/test/kotlin/io/archinaut/tenter/ArchitectureTest.kt` enforces the dependency boundaries below, prevents BattleTech imports, and keeps managed `Panel` mutations behind `PanelSet`.

The packages have distinct jobs: `palette` owns semantic colors; `text` owns display-cell metrics; `screen` owns buffers, cells, styles, canvas painting, and `Layout`/`Constraint` rectangle splitting; `widget` owns the `Widget`, `MeasurableWidget`, and `StatefulWidget` contracts, the internal recorded layouts behind measurable widgets, and every widget (blocks, viewports, rows, gauges), with stateful panel composition (the `Panels` widget over its `PanelSet` state) in `widget.panel`; `animation` owns finite frame descriptions and elapsed-time playback; `input` owns key maps and their help hints; `terminal` owns the double-buffered `Terminal`, its per-draw `Frame`, diff rendering, terminal event flows, and screen lifecycle.

The rendering model follows Ratatui: `Terminal.draw` hands a `Frame` to the caller, widgets render into a `Canvas` clipped to their `Rect`, and the terminal diffs the drawn `Buffer` against the previous one.

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
- [Scrolling and panels](site/panels.md): state ownership, panel widths, completed-frame observations, hidden panels, and failed-render recovery.
- [Input and resizing](site/input.md): key-map validation, event collection, application confinement, and terminal lifetime.
- [Widgets and styling](site/styling.md): glyph choices, display-cell sizing, and palette requirements.
- [Animation](site/animation.md): finite elapsed-time playback and caller scheduling.

## Implementation invariants

- Tenter adds primitives to Mordant rather than wrapping it. Preserve direct Mordant
  types in `TerminalEvent.Input` and `KeyBinding`, and take a Mordant terminal in `events`.
- `KeyMap` copies context, layer, and group collections, and `PanelSet` copies its panel
  lists. Do not expose mutable declaration collections.
- A panel's content builder runs once per rendered frame; widths come from the panel
  declaration, never from calling a builder. Keep mode-specific options out of the common
  render operation.
- Propagate reveal requests through composites such as `Stack`, `Columns`, and `.block()`,
  and resolve them before the actual viewport paint. Commit managed viewport state and hit geometry together
  only after all panels paint successfully. Expose no provisional or second settled-state read path.
- Share complete grapheme segmentation and display-width rules across text, wrapping,
  truncation, styled spans, and widgets. Cover combining marks, variation selectors,
  ZWJ sequences, flags, keycaps, decomposed accents, CJK, and supplementary characters.
  Clip wide glyphs atomically without leaving continuation cells behind. Reject or
  sanitize invalid controls and isolated surrogates according to each operation's contract;
  validate dimensions and glyph inputs before painting.
- Resolve fixed colors in both foreground and background channels. Palette validation
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
