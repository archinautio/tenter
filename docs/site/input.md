# Input and resizing

## Keep drawing and state updates together

Create a Tenter `Terminal` and enter its screen scope with
`Terminal().withScreen { terminal -> ... }`. Inside the block, call
`terminal.draw { frame -> ... }` for the initial frame. Collect input and resize events in one application execution context.
Update application state and repaint there as well.

`withScreen` restores the screen and cursor when its block returns or throws. Drawing
outside the scope throws, and so does a nested scope on the same terminal; entering again
after a scope returned is fine.
Keep only one event collection active per terminal. Collecting `terminal.events()`
manages raw mode, and stopping collection releases it before collection returns. The
flow starts producing events when collected. Avoid abrupt process termination inside the screen scope, which
bypasses normal cleanup.

## Reach the Mordant terminal

`Terminal()` creates the process's real Mordant terminal and exposes it as `terminal.backend`.
Pass your own as `Terminal(backend)` to configure Mordant yourself. Printing through `backend`
directly bypasses frame diffing; call `terminal.clear()` afterwards so the next draw repaints
fully. To test drawing without a real terminal, see [testing](testing.md).

## Collect events and repaint

`terminal.events()` delivers keyboard and mouse input and terminal resizes as one flow.
The compiled [interactive example](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleMain.kt)
collects it. This excerpt runs inside
`runInteractive`, after creating `app` and drawing the initial frame. The loop reads events
from and draws to the same `terminal`.

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/ExampleMain.kt:input-loop"
```

`TerminalEvent.Input` carries a Mordant input event. `Resized` supplies the new `Size` in cells.
To quit, stop collecting: the example's quit key sets `app.running` to false, and
`takeWhile` ends collection, which lets the terminal scope close. `terminal.draw` follows the backend's current size, so
drawing again after a resize is enough; `frame.area` already has the new dimensions.

## Map keys to application actions

Use `KeyMap` when you want bindings and help hints defined together. The following
is the complete key-map object from the example, with its imports. `ExampleContext`
and `ExampleAction` are the example application's own types.

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/ExampleKeyMap.kt:6:"
```

Resolve a keyboard event with `map.resolve(listOf(context), event)` and handle the
returned action in your application. A null result means no binding matched.
`KeyHelp(listOf(map.hints(context)))` displays hints from the same definition.

Keys match the Mordant event exactly, including modifiers. Use unique exact bindings
and nonblank hint ids. Group references must exist, sectionless credits must be
unambiguous, and a group with no bindings must be explicitly `bindingless`. Keep
action identity stable after constructing the map. Actions are your own type, typically
an enum, so `when` over `resolve`'s result is exhaustive. If the
application has several contexts, it chooses their precedence.
See the [key-map source](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleKeyMap.kt)
and [action handling](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleApp.kt).
