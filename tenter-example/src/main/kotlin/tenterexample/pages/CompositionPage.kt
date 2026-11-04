// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.pages

import io.archinaut.tenter.widget.Title
import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.rendering.BorderType
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.VerticalAlign
import io.archinaut.tenter.input.HintGroup
import io.archinaut.tenter.input.KeyBinding
import io.archinaut.tenter.input.KeyLayer
import io.archinaut.tenter.input.KeySection
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Constraint
import io.archinaut.tenter.screen.Padding
import io.archinaut.tenter.screen.Size
import io.archinaut.tenter.text.CellWidth
import io.archinaut.tenter.widget.Block
import io.archinaut.tenter.widget.Borders
import io.archinaut.tenter.widget.Flow
import io.archinaut.tenter.widget.MeasurableWidget
import io.archinaut.tenter.widget.Paragraph
import io.archinaut.tenter.widget.Row
import io.archinaut.tenter.widget.Stack
import io.archinaut.tenter.widget.Widget
import io.archinaut.tenter.widget.block
import io.archinaut.tenter.widget.revealed
import kotlin.time.Duration
import tenterexample.pages.PageSections.heading
import tenterexample.pages.PageSections.note

/** Block borders and padding, Stack, Row constraints, Flow, and Rect.aligned, with keys cycling their settings. */
internal class CompositionPage : ShowcasePage<CompositionPage.State, CompositionPage.Action> {

    internal data class State(
        val borders: Int = 0,
        val borderType: Int = 0,
        val padding: Int = 1,
        val spacing: Int = 1,
        val placement: Int = PLACEMENTS.indexOf(TextAlign.CENTER to VerticalAlign.MIDDLE),
    )

    internal enum class Action { BORDERS, BORDER_TYPE, PADDING, SPACING, PLACEMENT }

    override val title: String = "Composition"

    override val initialState: State = State()

    private val keys = PageKeys(
        KeyLayer(
            title = "COMPOSITION",
            bindings = listOf(
                KeyBinding(KeyboardEvent("b"), Action.BORDERS, "borders"),
                KeyBinding(KeyboardEvent("t"), Action.BORDER_TYPE, "type"),
                KeyBinding(KeyboardEvent("p"), Action.PADDING, "padding"),
                KeyBinding(KeyboardEvent("s"), Action.SPACING, "spacing"),
                KeyBinding(KeyboardEvent("a"), Action.PLACEMENT, "placement"),
            ),
            hintGroups = listOf(
                HintGroup("borders", "b", "cycle which sides the block borders"),
                HintGroup("type", "t", "cycle the border glyphs"),
                HintGroup("padding", "p", "cycle the block's padding"),
                HintGroup("spacing", "s", "cycle the spacing of Stack, Row, and Flow"),
                HintGroup("placement", "a", "cycle where Rect.aligned places the inner box"),
            ),
        ),
    )

    override fun resolve(state: State, event: KeyboardEvent): Action? = keys.resolve(event)

    override fun hints(state: State): List<KeySection> = keys.hints

    override fun handle(state: State, action: Action, now: Duration): State = when (action) {
        Action.BORDERS -> state.copy(borders = (state.borders + 1) % BORDERS.size)
        Action.BORDER_TYPE -> state.copy(borderType = (state.borderType + 1) % BORDER_TYPES.size)
        Action.PADDING -> state.copy(padding = (state.padding + 1) % CYCLE_LENGTH)
        Action.SPACING -> state.copy(spacing = (state.spacing + 1) % CYCLE_LENGTH)
        Action.PLACEMENT -> state.copy(placement = (state.placement + 1) % PLACEMENTS.size)
    }

    override fun view(state: State, now: Duration): PageView {
        val borders = BORDERS[state.borders]
        val (typeName, borderType) = BORDER_TYPES[state.borderType]
        val block = Block(
            title = Title("Block"),
            borders = borders,
            padding = Padding.uniform(state.padding),
            borderType = borderType,
        )
        val spacing = state.spacing
        val (horizontal, vertical) = PLACEMENTS[state.placement]
        // --8<-- [start:columns]
        val columns = Row(
            listOf(
                Constraint.Length(14) to box("Length(14)"),
                Constraint.Percentage(30) to box("Percentage(30)"),
                Constraint.Fill(1) to box("Fill(1)"),
            ),
            spacing = spacing,
        )
        // --8<-- [end:columns]
        return PageView(
            Stack(
                listOf(
                    heading("Block: $borders, $typeName glyphs, padding ${state.padding}"),
                    Row(
                        listOf(
                            Constraint.Fill(1) to Paragraph(BLOCK_TEXT, wrap = true).block(block),
                            Constraint.Length(FRAMED_WIDTH) to FRAMED,
                        ),
                        spacing = 2,
                    ),
                    heading("Stack, spacing $spacing"),
                    Stack(listOf(box("first"), box("second"), box("third")), spacing = spacing),
                    heading("Row: Length(14), Percentage(30), Fill(1)"),
                    columns,
                    heading("Flow"),
                    Flow(TAGS.map { Constraint.Length(CellWidth.of(it) + BOX_INSETS) to box(it) }, spacing = spacing),
                    heading("Rect.aligned: ${horizontal.name.lowercase()}, ${vertical.name.lowercase()}"),
                    MeasurableWidget { writer -> writer.draw(AlignedBox(horizontal, vertical), ALIGNED_HEIGHT) },
                    note("Maximize this panel with + or resize the terminal: Flow moves its chips between bands."),
                ),
            ),
        )
    }

    private fun box(label: String): MeasurableWidget = Paragraph(label).block(Block.bordered(padding = Padding.horizontal(1)))

    /** A bordered area with a smaller box placed inside it by [horizontal] and [vertical]. */
    private class AlignedBox(private val horizontal: TextAlign, private val vertical: VerticalAlign) : Widget {
        override fun render(canvas: Canvas) {
            val outer = Block.bordered()
            outer.render(canvas)
            val area = outer.inner(canvas.area).aligned(ALIGNED_SIZE, horizontal, vertical)
            Paragraph("here").block(Block.bordered(padding = Padding.horizontal(1))).render(canvas.region(area))
        }
    }

    internal companion object {
        // --8<-- [start:padding]
        internal val FRAMED: MeasurableWidget = Paragraph("OK").revealed().block(Block.bordered(padding = Padding.uniform(1)))
        // --8<-- [end:padding]

        private const val FRAMED_WIDTH: Int = 6
        private const val CYCLE_LENGTH: Int = 3
        private const val ALIGNED_HEIGHT: Int = 7
        private val ALIGNED_SIZE: Size = Size(8, 3)

        private val PLACEMENTS: List<Pair<TextAlign, VerticalAlign>> =
            listOf(VerticalAlign.TOP, VerticalAlign.MIDDLE, VerticalAlign.BOTTOM).flatMap { vertical ->
                listOf(TextAlign.LEFT, TextAlign.CENTER, TextAlign.RIGHT).map { it to vertical }
            }

        /** A boxed label's borders and padding, in cells. */
        private const val BOX_INSETS: Int = 4

        private const val BLOCK_TEXT: String =
            "The block paints only its chrome; the content inside keeps its own size and is laid out in what is left."

        private val BORDERS: List<Borders> = listOf(
            Borders.ALL,
            Borders.TOP + Borders.BOTTOM,
            Borders.LEFT + Borders.RIGHT,
            Borders.TOP,
            Borders.NONE,
        )

        private val BORDER_TYPES: List<Pair<String, BorderType>> = listOf(
            "rounded" to BorderType.ROUNDED,
            "square" to BorderType.SQUARE,
            "double" to BorderType.DOUBLE,
            "heavy" to BorderType.HEAVY,
            "ASCII" to BorderType.ASCII,
        )

        private val TAGS: List<String> = listOf("kotlin", "terminal", "mordant", "layout", "widgets", "jvm", "tui", "panels")
    }
}
