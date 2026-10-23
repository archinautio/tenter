# Testing

## Test your frames, not escape output

Your tests should check what your application draws, not how Tenter turns it into terminal
escape sequences. Tenter only sends the cells that changed since the previous frame, so text
in the raw output can be split or missing even when the frame is right.

Create a headless terminal with `Terminal.headless(width, height) { ... }` and pass it to your
application in place of `Terminal()`. It never touches a real terminal and follows the same
rules: your application enters `withScreen` and draws through the `Screen`. It reports
`AnsiLevel.NONE` and ignores the palette.

## Check what was drawn

`screen.draw { ... }` returns a `CompletedFrame`. When your own loop draws, read
`terminal.frame` afterwards: it is what the terminal shows, blank before the first draw, and it
keeps the last frame after the screen scope returns. `frame.lines()` gives each row's text, and
`frame.get(x, y)` gives a cell with its symbol and style. Styles hold color roles, so a test does
not depend on the palette. A headless terminal's frames never change after they are drawn.

## Script the input

The block passed to `Terminal.headless` is the user at the keyboard. It runs when your
application first collects `screen.events()`, which starts with a `Resized` event, and it sends
input with `key(...)`, `send(event)` and `resize(width, height)`. Each call returns once your
application asks for its next event, so by then it has handled the event and drawn; the script
reads `frame` to check the result. When the script returns, the events flow completes. If your
application stops reading (on its quit key, say), the rest of the script never runs, so check
the final frame after the application returns.

The script carries on across collections, like a real user's input: an application that reads
events in one loop and then in another sees the rest of the script in the second one. Operators
that buffer between `events()` and your handler, such as `merge`, let a call return as soon as
the event is queued.

The hello example's test checks that the greeting is on screen before the first key is read,
and that hello stops reading at `q`:

```kotlin
--8<-- "tenter-example/src/test/kotlin/tenterexample/HelloExampleTest.kt:test"
```

To test a palette, check its colors directly, for example `palette.foreground(ChromeRole.ACCENT)`.
