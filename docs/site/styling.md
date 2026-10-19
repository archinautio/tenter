# Widgets and styling

## Draw selectable rows

`SelectableRow` is a content widget that combines a label, optional check state, and
selection highlighting. A selected row requests visibility, so an enclosing viewport
follows it. This excerpt from the compiled [list example](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleListView.kt)
is inside a measurable widget's loop and draws each row with its text cursor
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

Tenter does not require Nerd Fonts. Text widths account for wide characters and
combined Unicode characters. Prefer terminal-cell measurements over Kotlin string
length when allocating widths.

## Apply colors

Start with `DefaultRolePalette` in `backend.withScreen(DefaultRolePalette)`.
Use `Style(ChromeRole.ACCENT)` when writing highlighted text, for example as
the second argument to `cursor.writeLine`. The default palette supplies the toolkit's
`ChromeRole` colors using the terminal's ANSI-16 palette.

For custom colors, construct `MapRolePalette` with a name, an `AnsiLevel`, a
`defaultBackground`, and a map from roles to `PaletteColor` values. Include every
`ChromeRole`, plus any application-specific roles. All colors must match the
chosen tier: `Ansi16`, `Xterm256`, or `TrueColor`.

A simple customization is to start with
`ChromeRole.entries.associateWith { DefaultRolePalette.foreground(it) }`, replace
an entry such as `ChromeRole.ACCENT`, and supply that map with `AnsiLevel.ANSI16`
and `DefaultRolePalette.defaultBackground`. Pass the new palette to `withScreen`.
Keep the palette stable for that terminal's lifetime. `AnsiLevel.NONE` is a terminal
capability, not a palette authoring tier; do not use it to construct a map palette.

For a color that must not change with the theme, use a `PaletteColor` directly as
the role, as Ratatui uses `Color::Rgb`: `Style(fg = PaletteColor.TrueColor(255, 128, 0))`.
The terminal renders it as given, without consulting the palette.

If the terminal reports no color support, Tenter suppresses color escapes while
keeping text and layout usable.
