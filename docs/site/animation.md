# Animation

Tenter describes finite animations and samples them at an elapsed time. Your
application supplies the clock, chooses where to draw, and schedules repainting.

## Describe the frames

Implement `Animation` with a size, frame count, frame duration, and a `frame(index)`
method returning a `View`. This class from the compiled
[animation example](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/ExampleAnimation.kt)
shows `A` and then `B`, for 100 milliseconds each. The source link includes its imports.

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/ExampleAnimation.kt:animation"
```

`GlyphGrid` is useful for small character-based frames. Here the grid has three cells
and places the animated glyph in the middle.

## Sample and draw

Create an `AnimationPlayback` with a list of `AnimationPlayback.Clip` values. Each
clip contains an animation and an application value, such as the destination id.
Call `playback.sample(elapsed)` using time elapsed from the animation's start.

The returned `frames` contain the currently visible content and its associated value.
Draw each frame's `content` into the canvas chosen by your application, then render
the completed screen. Use `nextChangeIn` to schedule the next repaint; null means
there is no upcoming change.

The example samples at `0.milliseconds`, `100.milliseconds`, and `200.milliseconds`.
These produce `A`, `B`, and no visible frames respectively. The animation is complete
at its exact end time. Use a monotonic clock for elapsed time and stop scheduling
updates when the animation completes or the application exits.

The demo's headless check exercises playback; its interactive catalog does not run
an animation timer. See [running the examples](examples.md).
