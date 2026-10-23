# Testing

## Test your frames, not escape output

Your tests should check what your application draws, not how Tenter turns it into terminal
escape sequences. Tenter only sends the cells that changed since the previous frame, so text
in the raw output can be split or missing even when the frame is right.

Create a headless terminal with `Terminal.headless(width, height)`. It has a fixed size, never
touches a real terminal, and draws without color, also outside `withScreen`. It reports
`AnsiLevel.NONE`, and its `events()` flow is empty, so pass input to your application yourself.

## Check what was drawn

`terminal.draw { ... }` returns a `CompletedFrame`. When your own loop draws, read
`terminal.lastFrame` afterwards: it is the frame the latest successful draw returned.
`frame.lines()` gives each row's text, and `frame.get(x, y)` gives a cell with its symbol
and style. Styles hold color roles, so a test does not depend on the palette.

The hello example's test passes a headless terminal and its own input, and checks that the
greeting is on screen before the first key is read:

```kotlin
--8<-- "tenter-example/src/test/kotlin/tenterexample/HelloExampleTest.kt:test"
```

To test a palette, check its colors directly, for example `palette.foreground(ChromeRole.ACCENT)`.
