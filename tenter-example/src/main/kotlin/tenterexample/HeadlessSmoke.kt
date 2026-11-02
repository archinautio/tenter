// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample

import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.MouseEvent
import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.screen.Point
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Size
import io.archinaut.tenter.terminal.CompletedFrame
import io.archinaut.tenter.terminal.HeadlessTerminal
import io.archinaut.tenter.terminal.Terminal
import io.archinaut.tenter.widget.Viewport
import io.archinaut.tenter.widget.ViewportState
import io.archinaut.tenter.widget.Widget
import io.archinaut.tenter.widget.panel.PanelMode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import tenterexample.colors.SchemeChoice
import tenterexample.colors.ShowcaseScheme
import tenterexample.pages.ColorsPage
import tenterexample.pages.CompositionPage
import tenterexample.shell.ShellPanelId
import tenterexample.shell.Showcase

/**
 * Runs the separate-consumer smoke scenario through public seams only: every page at three
 * sizes, driven by keys and stepped time, then the panel modes, the title, the marquee, flash
 * messages, and the scheme popup.
 */
public fun runHeadlessSmoke(): SmokeResult {
    checkFramedLabel()
    checkSchemes()
    val renders = SIZES.sumOf(::exercisePages)
    checkPanelModes()
    checkTitle()
    val marqueeMoved = checkMarquee()
    checkFlash()
    checkPopup()
    return SmokeResult(pageRenders = renders, marqueeMoved = marqueeMoved)
}

private val POPUP_SIZE: Size = Size(40, 8)

private val SIZES: List<Size> = listOf(Size(100, 30), Size(80, 24), Size(20, 5))

/** Keys that between them reach every page's bindings; q, which quits, is left out. */
private val PAGE_KEYS: List<String> =
    listOf("ArrowDown", "ArrowUp", "ArrowRight", "ArrowLeft", " ") + ('a'..'z').filter { it != 'q' }.map(Char::toString)

/** A headless showcase on [size] and the clock it reads. */
private class Harness(size: Size) {
    val time = TestTimeSource()
    val showcase = Showcase(SchemeChoice.Automatic(ShowcaseScheme.ANSI16), time)
    val terminal: HeadlessTerminal = Terminal.headless(size.width, size.height)

    fun draw(): CompletedFrame = terminal.withScreen { it.draw(showcase::render) }

    fun press(key: String, shift: Boolean = false) {
        showcase.handle(KeyboardEvent(key, shift = shift))
    }

    fun open(title: String) {
        showcase.select(showcase.pageTitles.indexOf(title))
        showcase.panels.focus(ShellPanelId.Demo(showcase.currentPage))
    }
}

private fun checkFramedLabel() {
    val framed = CompositionPage.FRAMED
    val frame = Terminal.headless(8, 5).withScreen { screen -> screen.draw { it.renderWidget(framed, it.area) } }
    check(frame.find("OK") == Point(2, 2)) { "the block did not place the paragraph inside its border and padding" }
    val followed = ViewportState()
    Terminal.headless(8, 3).withScreen { screen -> screen.draw { it.renderStatefulWidget(Viewport(framed), it.area, followed) } }
    check(followed.settled?.content?.height == 5) { "the block did not add its border and padding" }
    check(followed.settled?.revealed?.y == 2) { "the block lost the child's reveal" }
}

private fun checkSchemes() {
    ShowcaseScheme.entries.forEach { scheme ->
        check(scheme.build().foreground(StandardRole.ACCENT).space == scheme.space) {
            "the ${scheme.label} scheme does not use its own color space"
        }
    }
}

/** Opens every page on a [size] terminal, presses every key on it with time passing, and redraws. */
private fun exercisePages(size: Size): Int {
    val harness = Harness(size)
    var renders = 0
    harness.draw()
    harness.press("Enter")
    for (title in harness.showcase.pageTitles) {
        check(harness.showcase.pageTitles[harness.showcase.currentPage] == title) { "the next-page key skipped $title" }
        var frame = harness.draw()
        for (key in PAGE_KEYS) {
            harness.press(key)
            harness.time += 250.milliseconds
            frame = harness.draw()
        }
        check(frame.width == size.width && frame.height == size.height) { "the $title page drew at the wrong size" }
        if (size.width >= Showcase.WIDE_TERMINAL) {
            check(title.uppercase() in frame.text()) { "the $title page's panel is not titled" }
        }
        renders++
        harness.press("]")
    }
    return renders
}

/** Checks the layout: the page list left, the help right, a maximized panel alone, and the help minimized when narrow. */
private fun checkPanelModes() {
    val harness = Harness(Size(140, 30))
    harness.draw()
    val layout = checkNotNull(harness.showcase.panels.layout)
    val pages = checkNotNull(layout.slotOf(ShellPanelId.Pages)).outer
    val help = checkNotNull(layout.slotOf(ShellPanelId.Help)).outer
    check(pages.x == 0 && pages.width == Showcase.PAGES_WIDTH) { "the page list is not at the left" }
    check(help.x + help.width == 140 && help.width == Showcase.HELP_WIDTH) { "the help is not at the right" }

    harness.press("Enter")
    harness.press("+")
    harness.draw()
    val maximized = checkNotNull(harness.showcase.panels.layout).slots.single()
    check(maximized.id == ShellPanelId.Demo(0) && maximized.outer.width == 140) { "a maximized page did not take the body" }
    harness.press("-")
    check(harness.showcase.panels.modeOf(ShellPanelId.Demo(0)) == PanelMode.NORMAL) { "the page did not restore" }

    val narrow = Harness(Size(80, 24))
    narrow.draw()
    check(narrow.showcase.panels.modeOf(ShellPanelId.Help) == PanelMode.MINIMIZED) { "the help is not minimized on a narrow terminal" }
    check(narrow.showcase.panels.focused == ShellPanelId.Pages) { "minimizing the help moved the focus" }
}

/** The title is centered on the top row, gives its row up on a short terminal, and never schedules a redraw. */
private fun checkTitle() {
    val harness = Harness(Size(100, 30))
    val top = harness.draw().line(0)
    check(top.trim() == Showcase.TITLE_TEXT) { "the title is not on the top row" }
    check(top.indexOf(Showcase.TITLE_TEXT) == (100 - Showcase.TITLE_TEXT.length) / 2) { "the title is not centered" }
    check(harness.showcase.nextChangeIn == null) { "a still page schedules a redraw" }

    val short = Harness(Size(100, Showcase.TITLE_MIN_HEIGHT - 1))
    check(Showcase.TITLE_TEXT !in short.draw().text()) { "the title shows on a short terminal" }
}

/** The animation page's marquee moves as time passes and stands still while paused. */
private fun checkMarquee(): Boolean {
    val harness = Harness(Size(100, 30))
    harness.open("Animation")
    val first = harness.draw().text()
    check(harness.showcase.nextChangeIn != null) { "the marquee does not schedule its next frame" }
    harness.time += 120.milliseconds
    val second = harness.draw().text()

    harness.press("p")
    val paused = harness.draw().text()
    harness.time += 1.seconds
    check(harness.draw().text() == paused) { "the paused marquee kept moving" }
    check(harness.showcase.nextChangeIn == null) { "the paused marquee schedules a redraw" }
    return first != second
}

/** A flash appears in the status bar and is gone once its duration has passed. */
private fun checkFlash() {
    val harness = Harness(Size(100, 30))
    harness.open("Progress")
    harness.press("f")
    check("vanish" in harness.draw().line(1)) { "the flash message is not in the status bar" }
    harness.time += 3.seconds
    check("vanish" !in harness.draw().line(1)) { "the flash message outlived its duration" }
}

/** The colors page's popup, as the page returns it while open. */
private fun colorsPopup(): Widget {
    val page = ColorsPage(SchemeChoice.Automatic(ShowcaseScheme.ANSI16))
    return checkNotNull(page.view(ColorsPage.State(popup = true), Duration.ZERO).popup).content
}

/** The colors page's popup opens over its panel and closes again. */
private fun checkPopup() {
    val harness = Harness(Size(100, 30))
    harness.open("Colors")
    harness.press("o")
    val open = harness.draw()
    val title = checkNotNull(open.find("COLOR SCHEMES")) { "the scheme popup did not open" }
    // The block's title starts four cells into its top border.
    val popup = Rect(title.x - 4, title.y, POPUP_SIZE.width, POPUP_SIZE.height)
    harness.press("o")
    val closed = harness.draw()
    check("COLOR SCHEMES" !in closed.text()) { "the scheme popup did not close" }
    check(closed.region(popup).text().isNotBlank()) { "the popup no longer covers any of the page" }
    // Over cleared cells, the popup's area shows exactly what the popup draws on its own.
    val alone = Terminal.headless(popup.width, popup.height).withScreen { screen ->
        screen.draw { it.renderWidget(colorsPopup(), it.area) }
    }
    check(open.region(popup) == alone) { "the page shows through the popup" }
}
