// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.pages

import com.github.ajalt.mordant.input.KeyboardEvent
import io.archinaut.tenter.animation.AnimationPlayback
import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.input.HintGroup
import io.archinaut.tenter.input.KeyBinding
import io.archinaut.tenter.input.KeyLayer
import io.archinaut.tenter.input.KeySection
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.widget.MeasurableWidget
import io.archinaut.tenter.widget.Stack
import io.archinaut.tenter.widget.fixedSize
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import tenterexample.pages.PageSections.heading
import tenterexample.pages.PageSections.note

/** The "TENTER" marquee in block letters, at a speed the user sets, paused or playing. */
internal class AnimationPage : ShowcasePage<AnimationPage.State, AnimationPage.Action> {

    /** Frame 0 started at [anchor] on the page's clock; a paused marquee stays at [pausedAt]. */
    internal data class State(
        val frameDuration: Duration = 120.milliseconds,
        val anchor: Duration = Duration.ZERO,
        val pausedAt: Duration? = null,
    )

    internal enum class Action { PAUSE, SLOWER, FASTER }

    override val title: String = "Animation"

    override val initialState: State = State()

    private val keys = PageKeys(
        KeyLayer(
            title = "ANIMATION",
            bindings = listOf(
                KeyBinding(KeyboardEvent("p"), Action.PAUSE, "pause"),
                KeyBinding(KeyboardEvent("ArrowLeft"), Action.SLOWER, "speed"),
                KeyBinding(KeyboardEvent("ArrowRight"), Action.FASTER, "speed"),
            ),
            hintGroups = listOf(
                HintGroup("pause", "p", "pause or resume the marquee"),
                HintGroup("speed", "←/→", "slow down or speed up the frames"),
            ),
        ),
    )

    override fun resolve(state: State, event: KeyboardEvent): Action? = keys.resolve(event)

    override fun hints(state: State): List<KeySection> = keys.hints

    override fun handle(state: State, action: Action, now: Duration): State = when (action) {
        Action.PAUSE -> when (val pausedAt = state.pausedAt) {
            null -> state.copy(pausedAt = now)
            else -> state.copy(anchor = state.anchor + (now - pausedAt), pausedAt = null)
        }
        Action.SLOWER -> retimed(state, (state.frameDuration * 2).coerceAtMost(MAX_FRAME), now)
        Action.FASTER -> retimed(state, (state.frameDuration / 2).coerceAtLeast(MIN_FRAME), now)
    }

    override fun view(state: State, now: Duration): PageView {
        val animation = MarqueeAnimation(LETTERS, LETTERS_WIDTH, state.frameDuration, Style(StandardRole.ACCENT))
        val loop = state.frameDuration * animation.frameCount
        val elapsed = (state.pausedAt ?: now) - state.anchor
        val sample = AnimationPlayback(listOf(AnimationPlayback.Clip(animation, Unit)))
            .sample(elapsed.inWholeMicroseconds.mod(loop.inWholeMicroseconds).microseconds)
        val marquee = sample.frames.map { it.content.fixedSize(it.size.width, it.size.height) }
        val status = if (state.pausedAt == null) "playing" else "paused"
        return PageView(
            Stack(
                listOf(
                    heading("Marquee, ${state.frameDuration.inWholeMilliseconds} ms per frame, $status"),
                    Stack(marquee),
                    note(
                        "An Animation is a finite list of frames; this one is $LETTERS_WIDTH GlyphGrid " +
                            "frames, each the letters shifted one cell further left. The page samples an " +
                            "AnimationPlayback at the time elapsed since frame 0, modulo the loop, and returns " +
                            "when the next frame is due, so the showcase redraws only then.",
                    ),
                ),
            ),
            // A paused marquee does not change until the next key.
            nextChangeIn = sample.nextChangeIn.takeIf { state.pausedAt == null },
        )
    }

    /** [state] at a new [frameDuration], keeping the frame showing at [now]. */
    private fun retimed(state: State, frameDuration: Duration, now: Duration): State {
        val at = state.pausedAt ?: now
        val frames = floor((at - state.anchor) / state.frameDuration)
        return state.copy(frameDuration = frameDuration, anchor = at - frameDuration * frames)
    }

    private companion object {
        private val MIN_FRAME: Duration = 15.milliseconds
        private val MAX_FRAME: Duration = 960.milliseconds

        private const val LETTERS_WIDTH: Int = 48

        private val LETTERS: List<String> = listOf(
            "█████ █████ █   █ █████ █████ ████ ",
            "  █   █     ██  █   █   █     █   █",
            "  █   ████  █ █ █   █   ████  ████ ",
            "  █   █     █  ██   █   █     █  █ ",
            "  █   █████ █   █   █   █████ █   █",
        )
    }
}
