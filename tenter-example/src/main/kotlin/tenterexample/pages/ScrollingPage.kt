// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.pages

import io.archinaut.tenter.widget.Title
import com.github.ajalt.mordant.input.KeyboardEvent
import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.input.HintGroup
import io.archinaut.tenter.input.KeyBinding
import io.archinaut.tenter.input.KeyLayer
import io.archinaut.tenter.input.KeySection
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Constraint
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.widget.Block
import io.archinaut.tenter.widget.MeasurableWidget
import io.archinaut.tenter.widget.Paragraph
import io.archinaut.tenter.widget.Row
import io.archinaut.tenter.widget.Scrollbar
import io.archinaut.tenter.widget.Stack
import io.archinaut.tenter.widget.Viewport
import io.archinaut.tenter.widget.ViewportState
import io.archinaut.tenter.widget.Widget
import io.archinaut.tenter.widget.followingEnd
import io.archinaut.tenter.widget.revealed
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import tenterexample.pages.PageSections.heading
import tenterexample.pages.PageSections.note

/**
 * Two viewports of the page's own, each with scrollbars: a list that follows its selection and a
 * log that grows over time and follows its end while the reader is there.
 */
internal class ScrollingPage : ShowcasePage<ScrollingPage.State, ScrollingPage.Action> {

    /**
     * The viewports' states are the library's own mutable objects, kept for the page's lifetime;
     * the log has [logged] lines plus one per interval since [loggingSince], while it runs.
     */
    internal data class State(
        val selected: Int = 0,
        val logged: Int = 3,
        val loggingSince: Duration? = Duration.ZERO,
        val list: ViewportState = ViewportState(),
        val log: ViewportState = ViewportState(),
    )

    internal enum class Action { UP, DOWN, RECENTER, LOG_UP, LOG_DOWN, TOGGLE_LOG }

    override val title: String = "Scrolling"

    override val initialState: State = State()

    private val keys = PageKeys(
        KeyLayer(
            title = "SCROLLING",
            bindings = listOf(
                KeyBinding(KeyboardEvent("ArrowUp"), Action.UP, "move"),
                KeyBinding(KeyboardEvent("ArrowDown"), Action.DOWN, "move"),
                KeyBinding(KeyboardEvent("c"), Action.RECENTER, "recenter"),
                KeyBinding(KeyboardEvent("k"), Action.LOG_UP, "log"),
                KeyBinding(KeyboardEvent("j"), Action.LOG_DOWN, "log"),
                KeyBinding(KeyboardEvent("l"), Action.TOGGLE_LOG, "logging"),
            ),
            hintGroups = listOf(
                HintGroup("move", "↑/↓", "move the list's selection; its viewport follows"),
                HintGroup("recenter", "c", "center the selection in the list"),
                HintGroup("log", "k/j", "scroll the log; back at the end it follows again"),
                HintGroup("logging", "l", "pause or resume the log"),
            ),
        ),
    )

    override fun resolve(state: State, event: KeyboardEvent): Action? = keys.resolve(event)

    override fun hints(state: State): List<KeySection> = keys.hints

    override fun handle(state: State, action: Action, now: Duration): State {
        when (action) {
            Action.UP -> return state.copy(selected = (state.selected - 1).coerceAtLeast(0))
            Action.DOWN -> return state.copy(selected = (state.selected + 1).coerceAtMost(ITEM_COUNT - 1))
            Action.RECENTER -> state.list.requestRecenter()
            Action.LOG_UP -> state.log.scrollBy(0, -1)
            Action.LOG_DOWN -> state.log.scrollBy(0, 1)
            Action.TOGGLE_LOG -> return if (state.loggingSince == null) {
                state.copy(loggingSince = now)
            } else {
                state.copy(logged = lineCount(state, now), loggingSince = null)
            }
        }
        return state
    }

    override fun view(state: State, now: Duration): PageView {
        val items = MeasurableWidget { writer ->
            repeat(ITEM_COUNT) { index ->
                val label = if (index == WIDE_ITEM) WIDE_LABEL else "item $index"
                val line = Paragraph(
                    "${if (index == state.selected) "▶" else " "} $label",
                    Style(if (index == state.selected) StandardRole.ACCENT else StandardRole.TEXT_PRIMARY),
                )
                writer.draw(if (index == state.selected) line.revealed() else line)
            }
        }
        val lines = lineCount(state, now)
        val log = Paragraph((1..lines).joinToString("\n") { "[${it.toString().padStart(3)}] event $it" }).followingEnd()
        val listScroll = state.list.settled
        val logScroll = state.log.settled
        val following = logScroll == null || logScroll.offset.y >= logScroll.maxOffset.y
        val boxes = MeasurableWidget { writer ->
            writer.draw(
                Row(
                    listOf(
                        Constraint.Fill(1) to framed("LIST", items, state.list),
                        Constraint.Fill(1) to framed("LOG", log, state.log),
                    ),
                    spacing = 1,
                ),
            )
        }
        return PageView(
            Stack(
                listOf(
                    heading("Viewport and Scrollbar"),
                    boxes,
                    Paragraph(
                        "list offset ${listScroll?.offset?.y ?: 0}/${listScroll?.maxOffset?.y ?: 0}   " +
                            "log: $lines lines, ${if (following) "following its end" else "scrolled back"}" +
                            if (state.loggingSince == null) ", paused" else "",
                        Style(StandardRole.TEXT_MUTED),
                    ),
                    note(
                        "The selected item asks with revealed() to stay visible, and the log asks with " +
                            "followingEnd() to keep its last line in view. Item $WIDE_ITEM is wider than the " +
                            "list, so the list also scrolls sideways.",
                    ),
                ),
            ),
            nextChangeIn = nextLineIn(state, lines, now),
        )
    }

    private fun nextLineIn(state: State, lines: Int, now: Duration): Duration? {
        val since = state.loggingSince ?: return null
        if (lines >= MAX_LINES) return null
        val elapsed = now - since
        return LOG_INTERVAL * (floor(elapsed / LOG_INTERVAL) + 1) - elapsed
    }

    private fun lineCount(state: State, now: Duration): Int {
        val since = state.loggingSince ?: return state.logged
        return (state.logged + ((now - since) / LOG_INTERVAL).toInt()).coerceAtMost(MAX_LINES)
    }

    /** [content] scrolled in a [BOX_HEIGHT]-row bordered box. */
    private fun framed(title: String, content: MeasurableWidget, state: ViewportState): MeasurableWidget =
        MeasurableWidget { writer -> writer.draw(ScrollBox(title, content, state), BOX_HEIGHT) }

    /** A bordered [Viewport] whose scrollbars run along its right and bottom borders. */
    private class ScrollBox(
        private val title: String,
        private val content: MeasurableWidget,
        private val state: ViewportState,
    ) : Widget {
        override fun render(canvas: Canvas) {
            val block = Block.bordered(title = Title(title))
            block.render(canvas)
            val inner = block.inner(canvas.area)
            if (inner.isEmpty) return
            Viewport(content).render(canvas.region(inner), state)
            val scroll = state.settled ?: return
            Scrollbar.vertical(THUMB).render(canvas.region(Rect(canvas.width - 1, inner.y, 1, inner.height)), scroll)
            Scrollbar.horizontal(THUMB).render(canvas.region(Rect(inner.x, canvas.height - 1, inner.width, 1)), scroll)
        }
    }

    private companion object {
        private const val ITEM_COUNT: Int = 40
        private const val WIDE_ITEM: Int = 7
        private const val WIDE_LABEL: String = "item 7 has a label far wider than the list, so the list scrolls sideways"
        private const val BOX_HEIGHT: Int = 10
        private const val MAX_LINES: Int = 999
        private val LOG_INTERVAL: Duration = 700.milliseconds
        private val THUMB: Style = Style(StandardRole.ACCENT)
    }
}
