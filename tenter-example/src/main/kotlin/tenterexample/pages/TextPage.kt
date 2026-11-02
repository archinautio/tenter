// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.pages

import com.github.ajalt.mordant.input.KeyboardEvent
import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.input.HintGroup
import io.archinaut.tenter.input.KeyBinding
import io.archinaut.tenter.input.KeyLayer
import io.archinaut.tenter.input.KeySection
import io.archinaut.tenter.screen.Constraint
import io.archinaut.tenter.screen.Line
import io.archinaut.tenter.screen.Span
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.widget.Block
import io.archinaut.tenter.widget.MeasurableWidget
import io.archinaut.tenter.widget.Paragraph
import io.archinaut.tenter.widget.Row
import io.archinaut.tenter.widget.Stack
import io.archinaut.tenter.widget.VerticalText
import io.archinaut.tenter.widget.block
import kotlin.time.Duration
import tenterexample.pages.PageSections.heading
import tenterexample.pages.PageSections.note

/** Paragraph wrapping and ellipsizing at a width the user changes, styled spans, and vertical text. */
internal class TextPage : ShowcasePage<TextPage.State, TextPage.Action> {

    internal data class State(
        val width: Int = 36,
        val wrap: Boolean = true,
    )

    internal enum class Action { NARROWER, WIDER, TOGGLE_WRAP }

    override val title: String = "Text"

    override val initialState: State = State()

    private val keys = PageKeys(
        KeyLayer(
            title = "TEXT",
            bindings = listOf(
                KeyBinding(KeyboardEvent("ArrowLeft"), Action.NARROWER, "width"),
                KeyBinding(KeyboardEvent("ArrowRight"), Action.WIDER, "width"),
                KeyBinding(KeyboardEvent("w"), Action.TOGGLE_WRAP, "wrap"),
            ),
            hintGroups = listOf(
                HintGroup("width", "←/→", "narrow or widen the sample column"),
                HintGroup("wrap", "w", "wrap the paragraph or ellipsize each line"),
            ),
        ),
    )

    override fun resolve(state: State, event: KeyboardEvent): Action? = keys.resolve(event)

    override fun hints(state: State): List<KeySection> = keys.hints

    override fun handle(state: State, action: Action, now: Duration): State = when (action) {
        Action.NARROWER -> state.copy(width = (state.width - WIDTH_STEP).coerceAtLeast(MIN_WIDTH))
        Action.WIDER -> state.copy(width = (state.width + WIDTH_STEP).coerceAtMost(MAX_WIDTH))
        Action.TOGGLE_WRAP -> state.copy(wrap = !state.wrap)
    }

    override fun view(state: State, now: Duration): PageView {
        val mode = if (state.wrap) "wrapped" else "ellipsized"
        val sample = Paragraph(SAMPLE, wrap = state.wrap).block(Block.bordered(title = "${state.width} cells"))
        val rows = MeasurableWidget { writer ->
            writer.writeRow("tenter-0.2.0.jar", "312 KB", Style(StandardRole.TEXT_PRIMARY), Style(StandardRole.TEXT_MUTED))
            writer.writeRow("a left label long enough to give way to the value", "right", Style(StandardRole.TEXT_PRIMARY))
        }.block(Block.bordered(title = "writeRow"))
        return PageView(
            Stack(
                listOf(
                    heading("Paragraph, $mode"),
                    column(state.width, sample),
                    heading("Styled spans"),
                    Paragraph(listOf(SPANS)),
                    heading("Left and right values"),
                    column(state.width, rows),
                    heading("VerticalText"),
                    Row(
                        listOf(
                            Constraint.Length(VERTICAL_WIDTH) to VerticalText("TENTER", Style(StandardRole.ACCENT)),
                            Constraint.Length(VERTICAL_WIDTH) to VerticalText("中文"),
                            Constraint.Length(VERTICAL_WIDTH) to VerticalText("été"),
                        ),
                    ),
                    note(
                        "Widths are display cells: a wide glyph takes two, a decomposed accent one. " +
                            "Without wrapping, each line is cut with an ellipsis.",
                    ),
                ),
            ),
        )
    }

    /** [content] laid out [width] cells wide, whatever the panel's width. */
    private fun column(width: Int, content: MeasurableWidget): MeasurableWidget =
        Row(listOf(Constraint.Length(width) to content))

    private companion object {
        private const val MIN_WIDTH: Int = 8
        private const val MAX_WIDTH: Int = 80
        private const val WIDTH_STEP: Int = 2
        private const val VERTICAL_WIDTH: Int = 4

        private const val SAMPLE: String =
            "Tenter wraps text at word boundaries and splits a word wider than a row. " +
                "Wide glyphs such as 中文 take two cells; a decomposed é takes one.\n" +
                "A newline starts a new line."

        private val SPANS: Line = Line(
            Span("bold", Style(bold = true)),
            Span(" · "),
            Span("italic", Style(italic = true)),
            Span(" · "),
            Span("accent", Style(StandardRole.ACCENT)),
            Span(" · "),
            Span("success", Style(StandardRole.SUCCESS)),
            Span(" · "),
            Span("danger on one line", Style(StandardRole.DANGER, underline = true)),
        )
    }
}
