// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.pages

import com.github.ajalt.mordant.input.KeyboardEvent
import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.color.TerminalColor
import io.archinaut.tenter.input.HintGroup
import io.archinaut.tenter.input.KeyBinding
import io.archinaut.tenter.input.KeyLayer
import io.archinaut.tenter.input.KeySection
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Line
import io.archinaut.tenter.screen.Padding
import io.archinaut.tenter.screen.Size
import io.archinaut.tenter.screen.Span
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.widget.Block
import io.archinaut.tenter.widget.MeasurableWidget
import io.archinaut.tenter.widget.Paragraph
import io.archinaut.tenter.widget.Stack
import io.archinaut.tenter.widget.Widget
import kotlin.time.Duration
import tenterexample.colors.SchemeChoice
import tenterexample.colors.ShowcaseScheme
import tenterexample.pages.PageSections.heading
import tenterexample.pages.PageSections.note

/**
 * Every standard role and text attribute in the active scheme, colors that bypass it, and a popup,
 * drawn over cleared cells, listing the schemes to relaunch with.
 */
internal class ColorsPage(private val choice: SchemeChoice) : ShowcasePage<ColorsPage.State, ColorsPage.Action> {

    internal data class State(val popup: Boolean = false)

    internal enum class Action { TOGGLE_POPUP }

    override val title: String = "Colors"

    override val initialState: State = State()

    private val keys = PageKeys(
        KeyLayer(
            title = "COLORS",
            bindings = listOf(KeyBinding(KeyboardEvent("o"), Action.TOGGLE_POPUP, "schemes")),
            hintGroups = listOf(HintGroup("schemes", "o", "open or close the list of schemes")),
        ),
    )

    override fun resolve(state: State, event: KeyboardEvent): Action? = keys.resolve(event)

    override fun hints(state: State): List<KeySection> = keys.hints

    override fun handle(state: State, action: Action, now: Duration): State = when (action) {
        Action.TOGGLE_POPUP -> state.copy(popup = !state.popup)
    }

    override fun view(state: State, now: Duration): PageView = PageView(
        Stack(
            listOf(
                heading("Scheme ${choice.description}: ${choice.scheme.description}"),
                note(
                    "The scheme is chosen at launch: the one matching the terminal, unless " +
                        "--scheme=<name> names another. Press o to see this list in a popup.",
                ),
                heading("Schemes for --scheme=<name>"),
                Paragraph(schemeLines(choice)),
                heading("Standard roles"),
                ROLES,
                heading("Attributes"),
                Paragraph(listOf(ATTRIBUTES)),
                heading("Fixed colors"),
                FIXED_COLORS,
                note(
                    "A TerminalColor used as a role skips the scheme and is sent as written, even to a " +
                        "terminal that shows fewer colors.",
                ),
            ),
        ),
        popup = if (state.popup) Popup(Size(POPUP_WIDTH, ShowcaseScheme.entries.size + POPUP_EXTRA_ROWS), SchemeList(choice)) else null,
    )

    /** The schemes, the active one marked, and the option that picks one. */
    private class SchemeList(private val choice: SchemeChoice) : Widget {
        override fun render(canvas: Canvas) {
            val block = Block.bordered(title = "COLOR SCHEMES", padding = Padding.horizontal(1))
            block.render(canvas)
            val footer = listOf(
                Line(""),
                Line("Relaunch with --scheme=<name>", Style(StandardRole.INFO)),
                Line("o closes this list", Style(StandardRole.TEXT_MUTED)),
            )
            Paragraph(schemeLines(choice) + footer).render(block.inner(canvas))
        }
    }

    private companion object {
        private const val LABEL_WIDTH: Int = 9

        /** One line per scheme `--scheme` accepts: its name and how it is built, the one in use marked. */
        private fun schemeLines(choice: SchemeChoice): List<Line> = ShowcaseScheme.entries.map { scheme ->
            val active = scheme == choice.scheme
            Line(
                "${if (active) "▶" else " "} ${scheme.label.padEnd(LABEL_WIDTH)} ${scheme.description}",
                Style(if (active) StandardRole.ACCENT else StandardRole.TEXT_PRIMARY),
            )
        }
        private const val POPUP_WIDTH: Int = 40

        /** The popup's borders and its three footer rows. */
        private const val POPUP_EXTRA_ROWS: Int = 5

        private const val ROLE_NAME_WIDTH: Int = 22
        private const val SWATCH: String = "      "

        private val ROLES: MeasurableWidget = MeasurableWidget { writer ->
            for (role in StandardRole.entries) {
                writer.write(0, role.name, Style(role))
                writer.write(ROLE_NAME_WIDTH, SWATCH, Style(bg = role))
                writer.newLine()
            }
        }

        private val ATTRIBUTES: Line = Line(
            listOf(
                "bold" to Style(bold = true),
                "dim" to Style(dim = true),
                "italic" to Style(italic = true),
                "underline" to Style(underline = true),
                "inverse" to Style(inverse = true),
                "strikethrough" to Style(strikethrough = true),
            ).flatMap { (name, style) -> listOf(Span(name, style), Span("  ")) },
        )

        private const val GRADIENT_WIDTH: Int = 36
        private const val GRAYSCALE_START: Int = 232
        private const val GRAYSCALE_STEPS: Int = 24

        private val FIXED_COLORS: MeasurableWidget = MeasurableWidget { writer ->
            for (x in 0 until GRADIENT_WIDTH) {
                val t = x * 255 / (GRADIENT_WIDTH - 1)
                writer.write(x, " ", Style(bg = TerminalColor.TrueColor(t, 96, 255 - t)))
            }
            writer.newLine()
            for (step in 0 until GRAYSCALE_STEPS) {
                writer.write(step, " ", Style(bg = TerminalColor.Ansi256(GRAYSCALE_START + step)))
            }
            writer.newLine()
        }
    }
}
