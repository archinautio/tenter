// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.shell

import io.archinaut.tenter.widget.Title
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.input.InputEvent
import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.MouseEvent
import io.archinaut.tenter.input.KeySection
import io.archinaut.tenter.screen.Constraint
import io.archinaut.tenter.screen.Layout
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.terminal.Frame
import io.archinaut.tenter.widget.Clear
import io.archinaut.tenter.widget.KeyHelp
import io.archinaut.tenter.widget.MeasurableWidget
import io.archinaut.tenter.widget.Paragraph
import io.archinaut.tenter.widget.VerticalText
import io.archinaut.tenter.widget.panel.Panel
import io.archinaut.tenter.widget.panel.PanelMode
import io.archinaut.tenter.widget.panel.PanelSet
import io.archinaut.tenter.widget.panel.Panels
import io.archinaut.tenter.widget.revealed
import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.screen.Style
import kotlin.time.Duration
import kotlin.time.TimeSource
import tenterexample.colors.SchemeChoice
import tenterexample.pages.AnimationPage
import tenterexample.pages.ColorsPage
import tenterexample.pages.CompositionPage
import tenterexample.pages.KeysPage
import tenterexample.pages.Popup
import tenterexample.pages.ProgressPage
import tenterexample.pages.ScrollingPage
import tenterexample.pages.SelectionPage
import tenterexample.pages.TextPage

/**
 * The showcase application: a title and a status bar over three panels, the page list, the
 * current page, and the key help. It owns the pages' state and dispatches input to them; the
 * pages only compute. Time comes from [timeSource], so a test can step it.
 */
internal class Showcase(
    private val scheme: SchemeChoice,
    timeSource: TimeSource = TimeSource.Monotonic,
) {
    private val start = timeSource.markNow()

    private val entries: List<PageEntry<*, *>> = listOf(
        PageEntry(TextPage()),
        PageEntry(CompositionPage()),
        PageEntry(SelectionPage()),
        PageEntry(ScrollingPage()),
        PageEntry(ProgressPage()),
        PageEntry(ColorsPage(scheme)),
        PageEntry(AnimationPage()),
        PageEntry(KeysPage()),
    )

    internal val pageTitles: List<String> = entries.map { it.page.title }

    internal val panels: PanelSet<ShellPanelId, ShellInputs> = createPanelSet()

    /** The page shown in the middle panel. */
    internal var currentPage: Int = 0
        private set

    internal var running: Boolean = true
        private set

    /** When the last frame drawn next changes on its own, or null if it only changes on input. */
    internal var nextChangeIn: Duration? = null
        private set

    private var fittedToWidth: Boolean = false

    init {
        entries[currentPage].clock.show(Duration.ZERO)
    }

    private val current: PageEntry<*, *> get() = entries[currentPage]

    private val demo: ShellPanelId.Demo get() = ShellPanelId.Demo(currentPage)

    internal fun render(frame: Frame) {
        val now = now()
        fitToWidth(frame.area.width)
        val showTitle = frame.area.height >= TITLE_MIN_HEIGHT
        val (titleArea, statusArea, body) = Layout.vertical(
            Constraint.Length(if (showTitle) 1 else 0),
            Constraint.Length(1),
            Constraint.Fill(1),
        ).split(frame.area)

        frame.renderWidget(TITLE, titleArea)

        val page = current.view(now)
        val location = "Tenter showcase  •  ${currentPage + 1}/${entries.size} ${current.page.title}  •  Tab moves the focus"
        frame.renderWidget(StatusBar(location, page.flash, scheme.description), statusArea)
        // --8<-- [start:panel-render]
        val inputs = ShellInputs(pageTitles, currentPage, page, help())
        frame.renderStatefulWidget(Panels(inputs, panelAreas(body)), body, panels)
        // --8<-- [end:panel-render]
        page.popup?.let { renderPopup(frame, it) }

        nextChangeIn = page.nextChangeIn
    }

    // --8<-- [start:panel-layout]
    private fun panelAreas(area: Rect): Map<ShellPanelId, Rect> {
        val shown = listOf(ShellPanelId.Pages, demo, ShellPanelId.Help)
        // A maximized panel takes the whole area alone.
        shown.firstOrNull { panels.modeOf(it) == PanelMode.MAXIMIZED }?.let { return mapOf(it to area) }

        // The page fills whatever the page list and the help leave, each at its mode's width.
        val (pages, page, help) = Layout.horizontal(
            Constraint.Length(widthOf(ShellPanelId.Pages, PAGES_WIDTH)),
            Constraint.Fill(1),
            Constraint.Length(widthOf(ShellPanelId.Help, HELP_WIDTH)),
        ).split(area)
        return mapOf(ShellPanelId.Pages to pages, demo to page, ShellPanelId.Help to help)
    }

    private fun widthOf(id: ShellPanelId, normal: Int): Int =
        if (panels.modeOf(id) == PanelMode.MINIMIZED) MINIMIZED_WIDTH else normal
    // --8<-- [end:panel-layout]

    /** Draws [popup] centered over the page's panel, on cells cleared of what the panel drew. */
    private fun renderPopup(frame: Frame, popup: Popup) {
        val slot = panels.layout?.slotOf(demo) ?: return
        val area = slot.content.aligned(popup.size)
        frame.renderWidget(Clear, area)
        frame.renderWidget(popup.content, area)
    }

    /** The current page's keys, then those of the showcase active for the focused panel. */
    private fun help(): List<KeySection> {
        val contexts = when (panels.focused) {
            ShellPanelId.Pages -> listOf(ShellContext.PAGES, ShellContext.GLOBAL)
            else -> listOf(ShellContext.SCROLL, ShellContext.GLOBAL)
        }
        return current.hints() + contexts.map(ShellKeyMap.map::hints)
    }

    internal fun handle(event: InputEvent) {
        when (event) {
            is KeyboardEvent -> handleKey(event)
            is MouseEvent -> handleMouse(event)
        }
    }

    private fun handleKey(event: KeyboardEvent) {
        // The showcase's global keys come first, then the page's own while it has focus, then the
        // focused panel's: choosing a page or scrolling.
        val global = ShellKeyMap.map.resolve(GLOBAL, event)
        if (global != null) return perform(global)
        val focused = panels.focused
        if (focused is ShellPanelId.Demo && current.press(event, now())) return
        val contexts = if (focused == ShellPanelId.Pages) listOf(ShellContext.PAGES, ShellContext.SCROLL) else SCROLL
        ShellKeyMap.map.resolve(contexts, event)?.let(::perform)
    }

    private fun perform(action: ShellAction) {
        when (action) {
            ShellAction.NEXT_FOCUS -> cycleFocus(1)
            ShellAction.PREVIOUS_FOCUS -> cycleFocus(-1)
            ShellAction.NEXT_PAGE -> select((currentPage + 1).mod(entries.size))
            ShellAction.PREVIOUS_PAGE -> select((currentPage - 1).mod(entries.size))
            ShellAction.GROW_PANEL -> panels.cycleFocusedMode(1)
            ShellAction.SHRINK_PANEL -> panels.cycleFocusedMode(-1)
            ShellAction.QUIT -> running = false
            ShellAction.PAGE_ABOVE -> select((currentPage - 1).coerceAtLeast(0))
            ShellAction.PAGE_BELOW -> select((currentPage + 1).coerceAtMost(entries.lastIndex))
            ShellAction.OPEN_PAGE -> panels.focus(demo)
            ShellAction.SCROLL_UP -> panels.scroll(panels.focused, 0, -1)
            ShellAction.SCROLL_DOWN -> panels.scroll(panels.focused, 0, 1)
            ShellAction.PAGE_UP -> panels.page(panels.focused, -1)
            ShellAction.PAGE_DOWN -> panels.page(panels.focused, 1)
        }
    }

    private fun handleMouse(event: MouseEvent) {
        // --8<-- [start:mouse]
        val hit = panels.hitTest(event.x, event.y) ?: return
        val scrollDelta = when {
            event.wheelUp -> -WHEEL_STEP
            event.wheelDown -> WHEEL_STEP
            else -> null
        }
        if (scrollDelta != null) {
            panels.scroll(hit.id, 0, scrollDelta)
            return
        }
        if (!event.left) return
        panels.focus(hit.id)
        val point = hit.contentPoint ?: return
        when (hit.id) {
            ShellPanelId.Pages -> if (point.y in entries.indices) select(point.y)
            is ShellPanelId.Demo -> current.click(point, now())
            ShellPanelId.Help -> Unit
        }
        // --8<-- [end:mouse]
    }

    /** Shows page [index]; when the page had focus, the new one takes it. */
    internal fun select(index: Int) {
        if (index == currentPage) return
        val now = now()
        val pageFocused = panels.focused is ShellPanelId.Demo
        current.clock.hide(now)
        currentPage = index
        current.clock.show(now)
        if (pageFocused) panels.focus(demo)
    }

    private fun cycleFocus(delta: Int) {
        val order = listOf(ShellPanelId.Pages, demo, ShellPanelId.Help)
        panels.focus(order[(order.indexOf(panels.focused) + delta).mod(order.size)])
    }

    /** On the first frame, minimizes the help when the terminal is too narrow for three columns. */
    private fun fitToWidth(width: Int) {
        if (fittedToWidth) return
        fittedToWidth = true
        if (width >= WIDE_TERMINAL) return
        val focused = panels.focused
        panels.focus(ShellPanelId.Help)
        panels.cycleFocusedMode(-1)
        panels.focus(focused)
    }

    private fun now(): Duration = start.elapsedNow()

    private fun createPanelSet(): PanelSet<ShellPanelId, ShellInputs> {
        // --8<-- [start:panels]
        // The page list has no maximized content, so it only minimizes.
        val pages = Panel<ShellPanelId, ShellInputs>(
            id = ShellPanelId.Pages,
            titles = listOf(Title("PAGES")),
            normal = { input -> pageList(input) },
            minimized = { VerticalText("PAGES") },
        )
        // One panel per page, so each keeps its own scroll position and mode.
        val demos = pageTitles.mapIndexed { index, title ->
            Panel<ShellPanelId, ShellInputs>(
                id = ShellPanelId.Demo(index),
                titles = listOf(Title(title.uppercase())),
                normal = { input -> input.page.content },
                maximized = { input -> input.page.content },
            )
        }
        val help = Panel<ShellPanelId, ShellInputs>(
            id = ShellPanelId.Help,
            titles = listOf(Title("HELP")),
            normal = { input -> KeyHelp(input.help) },
            minimized = { VerticalText("HELP") },
            maximized = { input -> KeyHelp(input.help) },
        )
        return PanelSet(listOf(pages) + demos + help)
        // --8<-- [end:panels]
    }

    /** The page titles, the current one marked and kept in view. */
    private fun pageList(input: ShellInputs): MeasurableWidget = MeasurableWidget { writer ->
        input.titles.forEachIndexed { index, title ->
            val selected = index == input.current
            val line = Paragraph(
                "${if (selected) "▶" else " "} $title",
                Style(if (selected) StandardRole.ACCENT else StandardRole.TEXT_PRIMARY),
            )
            writer.draw(if (selected) line.revealed() else line)
        }
    }

    internal companion object {
        internal const val PAGES_WIDTH: Int = 18
        internal const val HELP_WIDTH: Int = 38
        internal const val MINIMIZED_WIDTH: Int = 7

        /** Below this width, the help starts minimized. */
        internal const val WIDE_TERMINAL: Int = 100

        /** Below this height, the title gives its row to the panels. */
        internal const val TITLE_MIN_HEIGHT: Int = 26

        internal const val TITLE_TEXT: String = "T E N T E R      S H O W C A S E"
        private val TITLE: Paragraph = Paragraph(TITLE_TEXT, Style(StandardRole.ACCENT, bold = true), align = TextAlign.CENTER)

        private const val WHEEL_STEP: Int = 2
        private val GLOBAL: List<ShellContext> = listOf(ShellContext.GLOBAL)
        private val SCROLL: List<ShellContext> = listOf(ShellContext.SCROLL)
    }
}
