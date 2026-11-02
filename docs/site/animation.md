# Animation

Tenter describes finite animations and samples them at an elapsed time. Your
application supplies the clock, chooses where to draw, and schedules repainting.

## Describe the frames

Implement `Animation` with a size, frame count, frame duration, and a `frame(index)`
method returning a `Widget`. This class from the showcase's compiled
[marquee](https://github.com/archinautio/tenter/blob/main/tenter-example/src/main/kotlin/tenterexample/pages/MarqueeAnimation.kt) moves rows of text from right to left,
one cell per frame, wrapping around. The source link includes its imports.

```kotlin
--8<-- "tenter-example/src/main/kotlin/tenterexample/pages/MarqueeAnimation.kt:animation"
```

`GlyphGrid` is useful for small character-based frames. Here each frame is a grid as wide as
the marquee, with every glyph written one cell further left than in the frame before.

## Sample and draw

Create an `AnimationPlayback` with a list of `AnimationPlayback.Clip` values. Each
clip contains an animation and an application value, such as the destination id.
Call `playback.sample(elapsed)` using time elapsed from the animation's start.

The returned `frames` contain the currently visible content and its associated value.
Inside `terminal.draw`, render each visible frame's `content` with
`frame.renderWidget(content, area)` into the area chosen by your application. Use `nextChangeIn` to schedule the next repaint; null means
there is no upcoming change.

The animation is complete at its exact end time, when `frames` is empty and `nextChangeIn` is
null. Use a monotonic clock for elapsed time and stop scheduling updates when the animation
completes or the application exits.

The [showcase](examples.md#run-the-showcase) plays the marquee in block letters on its
**Animation** page, where you can pause it and change its speed:

- To loop, it samples at the elapsed time modulo the animation's length.
- The page returns the `nextChangeIn` of what it shows, and the showcase's loop waits for the
  next input event or for that time, whichever comes first, as
  [its event loop](input.md#collect-events-and-repaint) shows.
- A paused marquee reports no next change, so nothing redraws until the next key.
