// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample

import io.archinaut.tenter.widget.Title
import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.MouseEvent
import io.archinaut.tenter.color.ColorSpace
import io.archinaut.tenter.terminal.Terminal
import io.archinaut.tenter.widget.Paragraph
import io.archinaut.tenter.widget.panel.Panel
import io.archinaut.tenter.widget.panel.PanelSet
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tenterexample.colors.SchemeChoice
import tenterexample.colors.ShowcaseScheme
import tenterexample.shell.ShellInputs
import tenterexample.shell.ShellPanelId
import tenterexample.shell.Showcase

internal class ShowcaseTest {

    private val time = TestTimeSource()
    private val showcase = Showcase(SchemeChoice.Automatic(ShowcaseScheme.ANSI16), time)
    private val terminal = Terminal.headless(width = 120, height = 30)

    private fun draw() = terminal.withScreen { it.draw(showcase::render) }

    private fun press(key: String, shift: Boolean = false) = showcase.handle(KeyboardEvent(key, shift = shift))

    @Test
    fun `the headless smoke renders every page at every size and drives the marquee`() {
        val result = runHeadlessSmoke()

        assertThat(result.pageRenders).isEqualTo(24)
        assertThat(result.marqueeMoved).isTrue()
    }

    @Test
    fun `tab cycles the focus through the page list, the page, and the help`() {
        draw()

        val focused = (1..3).map {
            press("Tab")
            showcase.panels.focused
        }
        press("Tab", shift = true)

        assertThat(focused).containsExactly(ShellPanelId.Demo(0), ShellPanelId.Help, ShellPanelId.Pages)
        assertThat(showcase.panels.focused).isEqualTo(ShellPanelId.Help)
    }

    @Test
    fun `arrows choose a page from the list and the brackets switch pages from anywhere`() {
        draw()
        press("ArrowDown")
        press("ArrowDown")
        assertThat(showcase.currentPage).isEqualTo(2)

        press("Enter")
        press("]")
        assertThat(showcase.currentPage).isEqualTo(3)
        assertThat(showcase.panels.focused).isEqualTo(ShellPanelId.Demo(3))

        press("[")
        press("[")
        press("[")
        press("[")
        assertThat(showcase.currentPage).isEqualTo(showcase.pageTitles.lastIndex)
    }

    @Test
    fun `clicking a title in the page list shows that page`() {
        draw()
        val list = checkNotNull(showcase.panels.layout?.slotOf(ShellPanelId.Pages)).content

        showcase.handle(MouseEvent(x = list.x + 1, y = list.y + 1 + 4, left = true))

        assertThat(showcase.pageTitles[showcase.currentPage]).isEqualTo("Progress")
    }

    @Test
    fun `a page keeps its state while another page shows`() {
        showcase.select(showcase.pageTitles.indexOf("Progress"))
        press("Enter")
        press("ArrowRight")
        press("ArrowRight")
        assertThat(draw().text()).contains("Gauge at 50%")

        press("]")
        draw()
        press("[")

        assertThat(draw().text()).contains("Gauge at 50%")
    }

    @Test
    fun `a hidden page's clock stands still`() {
        showcase.select(showcase.pageTitles.indexOf("Progress"))
        showcase.panels.focus(ShellPanelId.Demo(showcase.currentPage))
        press("f")
        assertThat(draw().line(1)).contains("vanish")

        press("]")
        time += 10.seconds
        draw()
        press("[")

        assertThat(draw().line(1)).contains("vanish")
        time += 3.seconds
        assertThat(draw().line(1)).doesNotContain("vanish")
    }

    @Test
    fun `the showcase schedules its next frame only while something moves`() {
        draw()
        assertThat(showcase.nextChangeIn).isNull()

        showcase.select(showcase.pageTitles.indexOf("Animation"))
        showcase.panels.focus(ShellPanelId.Demo(showcase.currentPage))
        draw()
        assertThat(showcase.nextChangeIn).isNotNull()

        press("p")
        draw()
        assertThat(showcase.nextChangeIn).isNull()
    }


    @Test
    fun `the global keys win over a page's own`() {
        showcase.select(showcase.pageTitles.indexOf("Keys"))
        showcase.panels.focus(ShellPanelId.Demo(showcase.currentPage))

        press("q")

        assertThat(showcase.running).isFalse()
    }

    @Test
    fun `a key the page leaves unbound scrolls its panel`() {
        val short = Terminal.headless(width = 120, height = 12)
        short.withScreen { it.draw(showcase::render) }
        showcase.panels.focus(ShellPanelId.Demo(0))

        press("ArrowDown")
        short.withScreen { it.draw(showcase::render) }

        val scroll = checkNotNull(showcase.panels.layout?.slotOf(ShellPanelId.Demo(0))?.scroll)
        assertThat(scroll.offset.y).isEqualTo(1)
    }

    @Test
    fun `the interactive loop draws, redraws on time, and stops at q`() {
        var framesWithoutInput = 0
        val scripted = Terminal.headless(width = 100, height = 30) {
            // The page list has focus: choose the animation page.
            repeat(ANIMATION_PAGE) { key("ArrowDown") }
            val first = frame.text()
            // The marquee moves on its own, so the loop wakes up without input.
            while (frame.text() == first) {
                delay(20.milliseconds)
            }
            framesWithoutInput++
            key("q")
        }

        scripted.withScreen { screen -> runInteractive(screen, Showcase(SchemeChoice.Automatic(ShowcaseScheme.ANSI16))) }

        assertThat(framesWithoutInput).isEqualTo(1)
    }

    private companion object {
        private const val ANIMATION_PAGE: Int = 6
    }


    @Test
    fun `the scheme follows the terminal unless the option names one`() {
        val automatic = SchemeChoice.from(emptyArray(), ColorSpace.TRUECOLOR)
        val noColor = SchemeChoice.from(emptyArray(), null)
        val explicit = SchemeChoice.from(arrayOf("--scheme=truecolor"), ColorSpace.ANSI256)
        val unknown = SchemeChoice.from(arrayOf("--scheme=sepia"), ColorSpace.ANSI256)

        assertThat(automatic).isEqualTo(SchemeChoice.Result.Chosen(SchemeChoice.Automatic(ShowcaseScheme.TRUECOLOR), null))
        assertThat(noColor).isEqualTo(SchemeChoice.Result.Chosen(SchemeChoice.Automatic(ShowcaseScheme.ANSI16), null))
        val chosen = explicit as SchemeChoice.Result.Chosen
        assertThat(chosen.choice).isEqualTo(SchemeChoice.Explicit(ShowcaseScheme.TRUECOLOR))
        assertThat(chosen.warning).contains("ansi256")
        assertThat((unknown as SchemeChoice.Result.Unknown).message).contains("ansi16, ansi256, truecolor")
    }

    @Test
    fun `each showcase builds its own panels, and a panel belongs to one set`() {
        val other = Showcase(SchemeChoice.Automatic(ShowcaseScheme.ANSI16), time)
        draw()
        Terminal.headless(width = 120, height = 30).withScreen { it.draw(other::render) }

        val shared = Panel<ShellPanelId, ShellInputs>(id = ShellPanelId.Pages, titles = listOf(Title("PAGES")), normal = { Paragraph("") })
        PanelSet(listOf(shared))

        assertThat(other.panels).isNotSameAs(showcase.panels)
        assertThrows<IllegalArgumentException> { PanelSet(listOf(shared)) }
    }
}
