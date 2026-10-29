# Input and resizing

## Keep drawing and state updates together

Create a Tenter `Terminal` and enter its screen scope with
`Terminal().withScreen { screen -> ... }`. Inside the block, call
`screen.draw { frame -> ... }` for the initial frame. The block is inlined, so inside a
coroutine it can suspend, for example to collect `screen.events()` directly. Collect input and
resize events, update application state, and repaint, all in one execution context.

The screen scope follows these rules:

- `withScreen` restores the screen and cursor when its block returns or throws.
- Only the `Screen` draws and reads events, and it refuses to once its scope has returned.
- A nested scope on the same terminal throws; entering again after a scope returned is fine.
- Collecting `screen.events()` holds raw mode, and stopping collection releases it before
  collection returns.
- Keep only one event collection active at a time. A later collection continues reading the
  same input.
- Avoid terminating the process abruptly inside the scope, which bypasses cleanup.

## Configure the Mordant terminal

`Terminal()` creates the process's real Mordant terminal. Pass your own as `Terminal(mordant)`
to configure Mordant yourself, for example to force a color level. To test drawing and input
without a real terminal, see [testing](testing.md).

## Collect events and repaint

`screen.events()` delivers keyboard and mouse input and terminal resizes as one flow.
The compiled [interactive example](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleMain.kt)
collects it. This excerpt runs inside
`runInteractive`, after creating `app` and drawing the initial frame. The loop reads events
from and draws to the same `screen`.

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/ExampleMain.kt:input-loop"
```

- `TerminalEvent.Input` carries a Mordant input event.
- `TerminalEvent.Resized` supplies the new `Size` in cells. Drawing again is enough:
  `screen.draw` follows the terminal's size, and `frame.area` already has the new dimensions.
- To quit, stop collecting: the example's quit key sets `app.running` to false, and
  `takeWhile` ends collection, which lets the terminal scope close.

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

Building the map checks its rules, and throws when one is broken:

- Keys match the Mordant event exactly, including modifiers, so each binding must be unique
  within its layer.
- Hint ids must not be blank, and every group a binding references must exist.
- A binding on a layer without a title must credit a group on exactly one titled layer.
- A group with no bindings must be declared `bindingless`.

Actions are your own type, typically an enum, so `when` over `resolve`'s result is exhaustive;
keep their identity stable once the map is built. If the application has several contexts, it
chooses their precedence. See the [key-map source](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleKeyMap.kt)
and [action handling](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleApp.kt).
