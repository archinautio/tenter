# Architecture

Tenter splits the work with its callers:

- **Callers** describe prepared content, dispatch input intent, and inspect completed-frame
  observations.
- **Tenter** owns glyph integrity, layout size, scroll following, panel geometry, and
  terminal-scope cleanup.

Caller-facing contracts live in the public guides: [layouts](site/layouts.md),
[panels](site/panels.md), [input](site/input.md), [testing](site/testing.md),
[styling](site/styling.md), and [animation](site/animation.md). This page holds only what a
maintainer must preserve.

## Module and dependency boundaries

`tenter-example` depends on `tenter`, the published library.

- The library's main sources may depend only on the JDK, Kotlin, kotlinx coroutines, and Mordant.
- Application policies stay in consumers, such as input vocabulary, quit keys, wheel steps, and
  key glyphs.
- Mordant and coroutine types are deliberately public, so keep both as `api` dependencies.
- The example must build both as an ordinary module and as an isolated packaged-jar consumer.
- Rendering helpers for tests stay in Tenter's test sources, not its published artifact.

## Package layering

| Package | Owns | May use |
| --- | --- | --- |
| `color` | Semantic colors | — |
| `text` | Display-cell metrics | — |
| `input` | Key maps and their help hints | — |
| `screen` | Buffers, cells, styles, canvas painting, and `Layout`/`Constraint` rectangle splitting | `color`, `text` |
| `widget` | The widget contracts, the recorded layouts behind measurable widgets, and every widget; panels in `widget.panel` | `input`, `color`, `screen`, `text` |
| `animation` | Finite frame descriptions and elapsed-time playback | `screen`, `widget`, `text` |
| `terminal` | The real and headless `Terminal`s, their `Screen` and per-draw `Frame`, diff rendering, event flows, and screen lifecycle | `input`, `color`, `screen`, `widget` |

Two tests guard these boundaries:

- `LayeringTest` enforces the "May use" column.
- `ArchitectureTest` enforces the external dependency boundary, prevents BattleTech imports, and
  keeps managed `Panel` mutations behind `PanelSet`.

`Terminal` is sealed, and each kind opens an internal `Session` per scope that presents frames:

- A real terminal's session diffs against its previous frame and renders styles with the
  scope's color scheme.
- A headless terminal's session keeps each frame as a snapshot.

## Implementation invariants

- Tenter is a library over Mordant, not a framework around it. Preserve direct Mordant types
  in `TerminalEvent.Input` and `KeyBinding`.
- Only the scope's `Screen` draws, on every kind of terminal. Keep test-only observation
  (`frame`, the input script) on `HeadlessTerminal`, not on `Terminal`.
- `KeyMap` copies context, layer, and group collections, and `PanelSet` copies its panel
  list. Do not expose mutable declaration collections.
- Panels have no size. Do not reintroduce panel widths, layout factories, or a library-chosen
  arrangement, and keep mode-specific options out of the common render operation.
- Drawing flows one way:
    - Sizes and visibility requests travel up only as data in prepared layouts, and offsets
      travel down.
    - Never route a request through the canvas or its buffer, and never let a plain widget's
      paint decide one.
    - Composites never clip; only `Viewport` does.
- Keep scrolling responsibilities apart:
    - `ViewportState` chooses where a frame scrolls to.
    - `ScrollTarget` computes the offset.
    - `Viewport` measures and paints.
    - `Scrollbar` shows the settled position.
    - `Block` draws chrome only.
    - A panel composes them and records the geometry it painted, so hit testing reads the
      completed frame instead of re-deriving insets. Expose no provisional or second
      settled-state read path.
- Share complete grapheme segmentation and display-width rules across text, wrapping,
  truncation, styled spans, and widgets:
    - Cover combining marks, variation selectors, ZWJ sequences, flags, keycaps, decomposed
      accents, CJK, and supplementary characters.
    - Clip wide glyphs atomically, without leaving continuation cells behind.
    - Reject or sanitize invalid controls and isolated surrogates according to each
      operation's contract.
    - Validate dimensions and glyph inputs before painting.
- Colors:
    - Resolve fixed colors in both the foreground and background channels.
    - Validate color schemes eagerly.
    - Suppressing color on a terminal without it must not alter glyphs or layout.
- Screen lifecycle:
    - Screen entry changes the alternate screen and cursor only for interactive terminals.
    - Restore them on normal exit, entry failure, block failure, and cleanup failure, and
      suppress cleanup exceptions onto an existing failure.
    - Input cancellation uses bounded polling, so stopping event collection for any reason
      releases raw mode promptly.

Internal chrome spacing is not a public API. Preserve the supported boundaries
instead of introducing a public transaction manager, application event-loop abstraction,
theme hot-swap mechanism, or deep-copy promise as an incidental implementation change.
