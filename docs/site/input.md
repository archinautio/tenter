# Input and resizing

## Keep drawing and state updates together

Create a `Terminal`, enter `terminal.withScreen(DefaultRolePalette)`, and render an
initial frame. Collect input and resize events in one application execution context.
Update application state and repaint there as well.

`withScreen` restores the screen and cursor when its block returns or throws.
Keep only one input reader active per terminal. Input collection manages raw mode;
cancelling collection releases it. Input and resize flows start producing events
when collected. Avoid abrupt process termination inside the screen scope, which
bypasses normal cleanup.

## Collect events and repaint

The compiled [interactive example](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleMain.kt)
merges keyboard/mouse input with resize events. This excerpt runs inside
`runInteractive`, after creating `app`, observing the terminal `size`, and drawing
the initial frame. `renderer` comes from `withScreen`.

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/ExampleMain.kt:input-loop"
```

`TerminalEvent.Input` carries a Mordant input event. `Resized` supplies the new size.
The quit predicate turns `q` into `TerminalEvent.Quit`, ending collection and allowing
the terminal scope to close. Rebuild the frame using the new dimensions after a resize.

## Map keys to application actions

Use `KeyMap` when you want bindings and help hints defined together. The following
is the complete key-map object from the example, with its imports. `ExampleContext`
and `ExampleAction` are the example application's own types.

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/ExampleKeyMap.kt:6:"
```

Resolve a keyboard event with `map.resolve(listOf(context), event)` and handle the
returned action in your application. A null result means no binding matched.
`HelpView(listOf(map.hints(context)))` displays hints from the same definition.

Keys match the Mordant event exactly, including modifiers. Use unique exact bindings
and nonblank hint ids. Group references must exist, sectionless credits must be
unambiguous, and a group with no bindings must be explicitly `bindingless`. Keep
action identity and `InputAction.id` stable after constructing the map. If the
application has several contexts, it chooses their precedence.
See the [key-map source](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleKeyMap.kt)
and [action handling](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleApp.kt).
