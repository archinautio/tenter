# Widgets and styling

## Draw selectable rows

`SelectableRow` is a content widget that combines a label, optional check state, and
selection highlighting. A selected row requests visibility, so an enclosing viewport
follows it. This excerpt from the compiled [list example](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleListView.kt)
is inside a measurable widget's loop and draws each row with its row writer
`column`; `index`, `label`, and `state` describe the row currently being drawn.

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/ExampleListView.kt:rows"
```

Use `Checkbox` for a standalone checkbox, `Gauge` for a one-row ratio bar with an
optional label, `VerticalText` for a minimized panel stub, `FlashMessage` for a transient status
line, and `KeyHelp` for [keyboard hints](input.md#map-keys-to-application-actions).

## Choose glyphs

- `CheckboxGlyphs.DEFAULT` uses Unicode glyphs.
- `CheckboxGlyphs.ASCII` is a conservative choice for varied terminal fonts.
- `CheckboxGlyphs.NERD_FONT` requires an appropriate Nerd Font and is opt-in.
- Borders use Mordant's `BorderType` sets. `Block` and `Panel` default to
  `BorderType.ROUNDED`; pass `borderType = BorderType.ASCII` for terminals or fonts
  without box-drawing characters, or another set such as `SQUARE`, `HEAVY`, or `DOUBLE`.

Tenter does not require Nerd Fonts. Text widths account for wide characters and
combined Unicode characters. Prefer terminal-cell measurements over Kotlin string
length when allocating widths.

## Apply colors

`withScreen` draws with `ColorScheme.Default` unless you pass another color scheme.
Use `Style(StandardRole.ACCENT)` when writing highlighted text, for example as
the second argument to `writer.writeLine`. The default color scheme supplies the
toolkit's `StandardRole` colors using the terminal's ANSI-16 palette.

A style sets only the channels you give it. Writing `Style(fg = StandardRole.DANGER)` over
an already painted cell changes its glyph and foreground and keeps its background, so a
marker drawn over a colored area keeps the color under it. Set a channel explicitly to
replace it, including back to the default: `Style(bg = StandardRole.DEFAULT)` or
`Style(strikethrough = false)`. A channel no write sets renders as `StandardRole.DEFAULT`,
or with the attribute off.

Besides the two colors, a style carries the text attributes `bold`, `dim`, `italic`,
`underline`, `inverse`, and `strikethrough`, each a channel of its own:
`Style(fg = StandardRole.WARNING, bold = true)`. Tenter renders them through Mordant, so
each looks however the terminal draws that attribute; some terminals ignore italic or
render dim at normal intensity.

To draw a popup, such as a dialog or a banner, over content without it showing through, render
the `Clear` widget into the popup's area first: it resets every cell there to a blank that sets
no channel. Then render the popup into the same area:
`frame.renderWidget(Clear, area)` followed by `frame.renderWidget(popup, area)`.

For custom colors, construct `MapColorScheme` with a name, an `AnsiLevel`, a
`defaultBackground`, and a map from roles to `TerminalColor` values. Include every
`StandardRole`, plus any application-specific roles. All colors must match the
chosen tier: `Ansi16`, `Ansi256`, or `TrueColor`.

A simple customization is to start with
`StandardRole.entries.associateWith { ColorScheme.Default.foreground(it) }`, replace
an entry such as `StandardRole.ACCENT`, and supply that map with `AnsiLevel.ANSI16`
and `ColorScheme.Default.defaultBackground`. Pass the new color scheme as
`terminal.withScreen(colorScheme) { ... }`. To choose a color scheme from the terminal's
color support, read `terminal.ansiLevel` before entering the scope. A scope keeps its
color scheme; to switch themes, leave the scope and enter it again. `AnsiLevel.NONE` is a
terminal capability, not an authoring tier; do not use it to construct a `MapColorScheme`.

For a color that must not change with the theme, use a `TerminalColor` directly as
the role, as Ratatui uses `Color::Rgb`: `Style(fg = TerminalColor.TrueColor(255, 128, 0))`.
The terminal renders it as given, without consulting the color scheme.

If the terminal reports no color support, Tenter suppresses color escapes while
keeping text and layout usable.
