# Testing

## Test your frames, not escape output

Your tests should check what your application draws, not how Tenter turns it into terminal
escape sequences. Tenter only sends the cells that changed since the previous frame, so text
in the raw output can be split or missing even when the frame is right.

Create a headless terminal with `Terminal.headless(width, height) { ... }` and pass it to your
application in place of `Terminal()`. It never touches a real terminal and follows the same
rules: your application enters `withScreen` and draws through the `Screen`. Its
`colorSpace` is `null` and it ignores the color scheme.

## Check what was drawn

`screen.draw { ... }` returns a `CompletedFrame`. When your own loop draws, read
`terminal.frame` afterwards: it is what the terminal shows, blank before the first draw, and it
keeps the last frame after the screen scope returns. A headless terminal's frames never change
after they are drawn.

To test one widget, draw it on a headless terminal of its size:

```kotlin
val frame = Terminal.headless(9, 5).withScreen { screen ->
    screen.draw { it.renderWidget(widget, it.area) }
}
```

Pick the check that says what the test is about:

- **The whole content:** compare `frame.text()` with a raw string. It joins the rows with `\n`,
  trims each row's trailing spaces and drops the blank rows at the bottom, so the string holds
  only what was drawn:

  ```kotlin
  assertEquals(
      """
      |╭──╮
      |│hi│
      |╰──╯
      """.trimMargin(),
      frame.text(),
  )
  ```

- **One property:** `frame.get(x, y)` gives a cell with its symbol and style. Styles hold color
  roles, so a test does not depend on the color scheme.
- **Content and style together:** build the expected frame with `CompletedFrame.of(...)` from
  strings or styled `Line`s, and compare it with `assertEquals`. It is as wide as its widest line,
  so write out trailing spaces, or compare a `region` that holds only what matters:

  ```kotlin
  assertEquals(
      CompletedFrame.of(Line("hi", Style(fg = StandardRole.ACCENT))),
      frame.region(Rect(1, 1, 2, 1)),
  )
  ```

  Two frames are equal when they have the same size and every cell shows the same symbol in the
  same style as the terminal renders it: an unset color equals `StandardRole.DEFAULT` and an unset
  attribute equals off. Where a frame sits does not count, so a region equals a frame built with
  `of`. When they differ, the failure prints both frames as quoted rows followed by each change
  of style.
- **A large screen:** `frame.find(text)` gives the cell where the text starts, counting columns as
  the terminal does, so a wide character before it counts twice. Use it to locate content, then
  `region` to compare just that part, or `line(y)` to read one row.

## Script the input

The block passed to `Terminal.headless` is the user at the keyboard. It sends input with
`key(...)`, `send(event)` and `resize(width, height)`, and reads `frame` to check the result:

- It runs when your application first collects `screen.events()`, which starts with a
  `Resized` event.
- Each call returns once your application asks for its next event, so by then the application
  has handled the event and drawn.
- When the script returns, the events flow completes.
- If your application stops reading, on its quit key say, the rest of the script never runs,
  so check the final frame after the application returns.
- The script carries on across collections, like a real user's input: an application that
  reads events in one loop and then another sees the rest of the script in the second.
- Operators that buffer between `events()` and your handler, such as `merge`, let a call
  return as soon as the event is queued.

The hello example's test checks that the greeting is on screen before the first key is read,
and that hello stops reading at `q`:

```kotlin
--8<-- "tenter-example/src/test/kotlin/tenterexample/HelloExampleTest.kt:test"
```

To test a color scheme, check its colors directly, for example `colorScheme.foreground(StandardRole.ACCENT)`.
