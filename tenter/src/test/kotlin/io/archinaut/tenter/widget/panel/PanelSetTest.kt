// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget.panel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.screen.Constraint
import io.archinaut.tenter.screen.Layout
import io.archinaut.tenter.screen.Point
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.widget.MeasurableWidget
import io.archinaut.tenter.widget.Paragraph
import io.archinaut.tenter.widget.revealed
import io.archinaut.tenter.widget.fixedSize
import io.archinaut.tenter.widget.Widget

private enum class SetPanelId : PanelId { MAIN, A, B }

internal class PanelSetTest {

    @Test
    fun `a panel stuck to the bottom follows its growing content`() {
        var rows = 50
        val set = PanelSet(
            listOf(
                Panel<SetPanelId, Unit>(
                    SetPanelId.A,
                    "A",
                    normal = { MeasurableWidget { writer -> repeat(rows) { writer.writeLine("row$it") } } },
                    stickToBottom = true,
                ),
            ),
        )
        val first = render(set, setOf(SetPanelId.A)).offsetOf(SetPanelId.A).y

        rows = 60
        val grown = render(set, setOf(SetPanelId.A)).offsetOf(SetPanelId.A).y

        assertTrue(first > 0)
        assertEquals(first + 10, grown)
    }

    @Test
    fun `multiple hidden panels retain independent recenter requests until drawable`() {
        val ids = setOf(SetPanelId.A, SetPanelId.B)
        val set = PanelSet(ids.map { id ->
            Panel<SetPanelId, Unit>(id, id.name, normal = {
                MeasurableWidget { writer ->
                    repeat(100) {
                        val line = Paragraph("row$it")
                        writer.draw(if (it == 40) line.revealed() else line)
                    }
                }
            })
        })
        render(set, ids)
        ids.forEach { set.scroll(it, 0, -10) }
        val panned = render(set, ids)
        ids.forEach { assertEquals(10, panned.offsetOf(it).y) }
        ids.forEach(set::requestRecenter)

        assertTrue(render(set, emptySet()).slots.isEmpty())
        assertTrue(render(set, ids, width = 4).slots.all { it.scroll == null })
        val recentered = render(set, ids)

        ids.forEach { assertEquals(31, recentered.offsetOf(it).y) }
        ids.forEach { set.scroll(it, 0, -3) }
        val next = render(set, ids)
        ids.forEach { assertEquals(28, next.offsetOf(it).y) }
    }

    @Test
    fun `nondrawable slots publish no stale snapshot but retain pending scroll for recovery`() {
        for ((width, height) in listOf(4 to 12, 40 to 2, 0 to 0)) {
            val set = PanelSet(listOf(sidePanel(SetPanelId.A)))
            val visible = setOf(SetPanelId.A)
            render(set, visible, width = 40, height = 12)
            set.scroll(set.focused, 0, 3)
            val previous = render(set, visible, width = 40, height = 12)
            set.scroll(set.focused, 0, 2)

            val tiny = render(set, visible, width, height)

            assertNull(tiny.slots.single().scroll)
            assertNull(set.hitTest(2, 2)?.contentPoint)
            assertEquals(3, previous.offsetOf(SetPanelId.A).y)
            val restored = render(set, visible, width = 40, height = 12)
            assertEquals(5, restored.offsetOf(SetPanelId.A).y)
            assertEquals(36, checkNotNull(restored.slots.single().scroll).viewport.width)
        }
    }

    @Test
    fun `focused commands affect the retained hidden panel and become visible on return`() {
        val set = PanelSet(listOf(sidePanel(SetPanelId.A), sidePanel(SetPanelId.B)))
        set.focus(SetPanelId.B)
        render(set, setOf(SetPanelId.A, SetPanelId.B))
        render(set, emptySet())

        set.scroll(set.focused, 0, 3)
        set.page(set.focused, 1)
        set.cycleFocusedMode(1)

        assertEquals(SetPanelId.B, set.focused)
        assertEquals(PanelMode.NORMAL, set.modeOf(SetPanelId.A))
        assertEquals(PanelMode.MAXIMIZED, set.modeOf(SetPanelId.B))
        assertNull(set.hitTest(2, 2))
        // The application gives the maximized panel the whole area.
        val returned = render(set, setOf(SetPanelId.B))
        assertEquals(SetPanelId.B, returned.slots.single().id)
        // With no displayed viewport, a page request retains the existing one-row fallback.
        assertEquals(4, returned.offsetOf(SetPanelId.B).y)
    }

    @Test
    fun `only the panels given an area are drawn, whatever their mode`() {
        val set = PanelSet(listOf(sidePanel(SetPanelId.A), sidePanel(SetPanelId.B)))
        set.focus(SetPanelId.B)
        set.cycleFocusedMode(1)

        val layout = renderAreas(
            set,
            mapOf(SetPanelId.A to Rect(0, 0, 7, 24), SetPanelId.B to Rect(7, 0, 73, 24)),
        )

        assertEquals(PanelMode.MAXIMIZED, set.modeOf(SetPanelId.B))
        assertEquals(listOf(Rect(0, 0, 7, 24), Rect(7, 0, 73, 24)), layout.slots.map { it.outer })
    }

    @Test
    fun `failed later paint retains all offsets and queued scrolling and recenter until retry`() {
        var fail = false
        val a = Panel<SetPanelId, Unit>(SetPanelId.A, "A", normal = {
            MeasurableWidget { writer ->
                repeat(100) {
                    val line = Paragraph("row$it")
                    writer.draw(if (it == 40) line.revealed() else line)
                }
            }
        })
        val b = Panel<SetPanelId, Unit>(SetPanelId.B, "B", normal = {
            object : Widget {
                public override fun render(canvas: Canvas) { check(!fail) }
            }.fixedSize(20, 100)
        })
        val set = PanelSet(listOf(a, b))
        val visible = setOf(SetPanelId.A, SetPanelId.B)
        render(set, visible)
        set.scroll(SetPanelId.A, 0, -10)
        val previous = render(set, visible)
        val oldOffset = previous.offsetOf(SetPanelId.A)
        val oldHit = set.hitTest(2, 2)
        val oldBHit = set.hitTest(42, 2)
        set.requestRecenter(SetPanelId.A)
        set.scroll(SetPanelId.B, 0, 5)
        fail = true

        assertThrows(IllegalStateException::class.java) { render(set, visible) }

        assertEquals(oldOffset, previous.offsetOf(SetPanelId.A))
        assertEquals(oldBHit, set.hitTest(42, 2))
        assertEquals(oldHit, set.hitTest(2, 2))
        fail = false
        val retried = render(set, visible)
        val centered = retried.offsetOf(SetPanelId.A)
        assertTrue(centered != oldOffset)
        assertEquals(5, retried.offsetOf(SetPanelId.B).y)
        set.scroll(SetPanelId.A, 0, -3)
        val scrolled = render(set, visible)
        assertEquals(centered.y - 3, scrolled.offsetOf(SetPanelId.A).y)
        assertEquals(5, scrolled.offsetOf(SetPanelId.B).y)
    }

    @Test
    fun `failed first or later panel paints preserve completed hits and paging geometry`() {
        for (failedId in listOf(SetPanelId.A, SetPanelId.B)) {
            var fail = false
            val panels = listOf(SetPanelId.A, SetPanelId.B).map { id ->
                Panel<SetPanelId, Unit>(id, id.name, normal = {
                    object : Widget {
                        override fun render(canvas: Canvas) {
                            if (fail && id == failedId) error("paint failed")
                            canvas.setString(0, 0, "content")
                        }
                    }.fixedSize(30, 100)
                })
            }
            val set = PanelSet(panels)
            val visible = setOf(SetPanelId.A, SetPanelId.B)
            render(set, visible)
            val oldHit = set.hitTest(2, 2)
            set.scroll(SetPanelId.A, 0, 5)
            fail = true

            assertThrows(IllegalStateException::class.java) { render(set, visible, width = 40, height = 10) }

            assertEquals(oldHit, set.hitTest(2, 2))
            set.page(set.focused, 1)
            fail = false
            assertEquals(27, render(set, visible).offsetOf(SetPanelId.A).y)
        }
    }

    @Test
    fun `a failed initial paint publishes no geometry and can be retried`() {
        var fail = true
        val panel = Panel<SetPanelId, Unit>(SetPanelId.A, "A", normal = {
            object : Widget {
                override fun render(canvas: Canvas) { check(!fail) }
            }.fixedSize(10, 10)
        })
        val set = PanelSet(listOf(panel))

        assertThrows(IllegalStateException::class.java) { render(set, setOf(SetPanelId.A)) }
        assertNull(set.hitTest(2, 2))
        fail = false
        render(set, setOf(SetPanelId.A))

        assertEquals(SetPanelId.A, set.hitTest(2, 2)?.id)
    }

    @Test
    fun `initial focus is the first declared panel and named operations own state changes`() {
        val set = PanelSet(listOf(mainPanel(), sidePanel(SetPanelId.A)))

        assertEquals(SetPanelId.MAIN, set.focused)
        set.focus(SetPanelId.A)
        set.cycleFocusedMode(1)

        assertEquals(SetPanelId.A, set.focused)
        assertEquals(PanelMode.MAXIMIZED, set.modeOf(SetPanelId.A))
    }

    @Test
    fun `every id-addressed operation rejects a panel the set does not declare`() {
        val set = PanelSet(listOf(mainPanel(), sidePanel(SetPanelId.A)))

        assertThrows(IllegalArgumentException::class.java) { set.focus(SetPanelId.B) }
        assertThrows(IllegalArgumentException::class.java) { set.page(SetPanelId.B, 1) }
        assertThrows(IllegalArgumentException::class.java) { set.scroll(SetPanelId.B, 0, 2) }
        assertThrows(IllegalArgumentException::class.java) { set.requestRecenter(SetPanelId.B) }
        assertThrows(IllegalArgumentException::class.java) { set.modeOf(SetPanelId.B) }
        assertThrows(IllegalArgumentException::class.java) {
            renderAreas(set, mapOf(SetPanelId.B to Rect(0, 0, 10, 10)))
        }

        assertEquals(SetPanelId.MAIN, set.focused)
    }

    @Test
    fun `focusing another panel demotes a maximized panel to its recorded state`() {
        val a = sidePanel(SetPanelId.A)
        val set = PanelSet(listOf(mainPanel(), a, sidePanel(SetPanelId.B)))
        set.focus(SetPanelId.A)
        set.cycleFocusedMode(-1)
        set.cycleFocusedMode(-1)

        set.focus(SetPanelId.B)

        assertEquals(PanelMode.MINIMIZED, set.modeOf(SetPanelId.A))
    }

    @Test
    fun `a full cycle while maximized does not replace the restore state`() {
        val a = sidePanel(SetPanelId.A)
        val set = PanelSet(listOf(a, sidePanel(SetPanelId.B)))
        set.focus(SetPanelId.A)
        set.cycleFocusedMode(-1)
        set.cycleFocusedMode(-1)
        set.cycleFocusedMode(3)
        set.focus(SetPanelId.B)

        assertEquals(PanelMode.MINIMIZED, set.modeOf(SetPanelId.A))
    }

    @Test
    fun `at most one panel is maximized after focus and cycle transitions`() {
        val a = sidePanel(SetPanelId.A)
        val b = sidePanel(SetPanelId.B)
        val set = PanelSet(listOf(a, b))

        set.focus(SetPanelId.A)
        set.cycleFocusedMode(1)
        set.focus(SetPanelId.B)
        set.cycleFocusedMode(1)

        assertEquals(PanelMode.NORMAL, set.modeOf(SetPanelId.A))
        assertEquals(PanelMode.MAXIMIZED, set.modeOf(SetPanelId.B))
    }

    @Test
    fun `an empty frame retains both hidden panel state and its focus`() {
        val a = sidePanel(SetPanelId.A)
        val set = PanelSet(listOf(a, sidePanel(SetPanelId.B)))
        set.focus(SetPanelId.B)
        set.cycleFocusedMode(1)

        val empty = render(set, emptySet())

        assertEquals(emptyList<PanelLayout.Slot<SetPanelId>>(), empty.slots)
        assertEquals(SetPanelId.B, set.focused)
        assertEquals(PanelMode.MAXIMIZED, set.modeOf(SetPanelId.B))
        render(set, setOf(SetPanelId.A, SetPanelId.B))
        assertEquals(SetPanelId.B, set.focused)
        assertEquals(PanelMode.MAXIMIZED, set.modeOf(SetPanelId.B))
    }

    @Test
    fun `a hidden focused panel yields to the first rendered one in declaration order`() {
        val set = PanelSet(listOf(mainPanel(), sidePanel(SetPanelId.A), sidePanel(SetPanelId.B)))
        set.focus(SetPanelId.B)

        render(set, setOf(SetPanelId.A, SetPanelId.MAIN))

        assertEquals(SetPanelId.MAIN, set.focused)
    }

    @Test
    fun `pending scroll does not change completed frame observations`() {
        val set = PanelSet(listOf(sidePanel(SetPanelId.A)))
        assertNull(set.hitTest(2, 2))

        val previous = render(set, setOf(SetPanelId.A))
        val hit = set.hitTest(2, 2)
        assertEquals(Point.ORIGIN, previous.offsetOf(SetPanelId.A))
        set.scroll(set.focused, 0, 3)

        assertEquals(Point.ORIGIN, previous.offsetOf(SetPanelId.A))
        assertEquals(hit, set.hitTest(2, 2))
        val scrolled = render(set, setOf(SetPanelId.A))
        assertEquals(Point(0, 3), scrolled.offsetOf(SetPanelId.A))
    }

    @Test
    fun `page scrolls the named panel by its viewport height regardless of focus`() {
        val set = PanelSet(listOf(sidePanel(SetPanelId.A), sidePanel(SetPanelId.B)))
        val visible = setOf(SetPanelId.A, SetPanelId.B)
        val pageHeight = render(set, visible, height = 10).slots.last().content.height

        set.page(SetPanelId.B, 1)
        val paged = render(set, visible, height = 10)

        assertEquals(SetPanelId.A, set.focused)
        assertEquals(pageHeight, paged.offsetOf(SetPanelId.B).y)
        assertEquals(0, paged.offsetOf(SetPanelId.A).y)
    }

    @Test
    fun `each rendered content builder runs once`() {
        var builds = 0
        val panel = sidePanel(SetPanelId.A, builds = { builds++ })
        val set = PanelSet(listOf(panel))

        render(set, setOf(SetPanelId.A))

        assertEquals(1, builds)
    }

    @Test
    fun `the set copies its panel list`() {
        val panel = sidePanel(SetPanelId.A)
        val input = mutableListOf(panel)
        val set = PanelSet(input)
        input.clear()

        // Emptying the caller's list must not empty the set: the declaration was copied.
        assertEquals(SetPanelId.A, render(set, setOf(SetPanelId.A)).slots.single().id)
        assertThrows(IllegalArgumentException::class.java) { PanelSet<SetPanelId, Unit>(emptyList()) }
    }

    @Test
    fun `duplicate ids and instances fail before attachment`() {
        val duplicateIdA = sidePanel(SetPanelId.A)
        val duplicateIdB = sidePanel(SetPanelId.A)
        assertThrows(IllegalArgumentException::class.java) {
            PanelSet(listOf(duplicateIdA, duplicateIdB))
        }

        assertThrows(IllegalArgumentException::class.java) {
            PanelSet(listOf(duplicateIdA, duplicateIdA))
        }
        val fresh = sidePanel(SetPanelId.B)
        assertThrows(IllegalArgumentException::class.java) {
            PanelSet(listOf(fresh, sidePanel(SetPanelId.B)))
        }
        PanelSet(listOf(fresh))
        assertThrows(IllegalArgumentException::class.java) { PanelSet(listOf(fresh)) }
    }

    @Test
    fun `a panel cannot be attached to two sets`() {
        val panel = sidePanel(SetPanelId.A)
        PanelSet(listOf(panel))

        val error = assertThrows(IllegalArgumentException::class.java) {
            PanelSet(listOf(panel))
        }

        assertEquals("Panel A is already attached to a PanelSet", error.message)
    }

    @Test
    fun `hit testing uses settled content geometry and ignores pending scroll`() {
        val panel = sidePanel(SetPanelId.A)
        val set = PanelSet(listOf(panel))

        val first = render(set, setOf(SetPanelId.A), width = 40, height = 12)
        val slot = first.slots.single()
        assertNull(set.hitTest(slot.content.x, slot.content.y)?.contentPoint)
        assertEquals(Point(0, 0), set.hitTest(slot.content.x, slot.content.y + 1)?.contentPoint)

        set.scroll(SetPanelId.A, 0, 3)
        assertEquals(
            Point(0, 0),
            set.hitTest(slot.content.x, slot.content.y + 1)?.contentPoint,
            "queued scroll must not change the displayed frame's mapping",
        )

        val scrolled = render(set, setOf(SetPanelId.A), width = 40, height = 12).slots.single()
        assertEquals(Point(0, 3), set.hitTest(scrolled.content.x, scrolled.content.y + 1)?.contentPoint)
        assertTrue(set.hitTest(scrolled.outer.x - 1, scrolled.outer.y) == null)
    }

    @Test
    fun `hit testing identifies a border without a content point`() {
        val set = PanelSet(listOf(mainPanel(), sidePanel(SetPanelId.A)))
        renderAreas(set, mapOf(SetPanelId.MAIN to Rect(0, 0, 40, 12), SetPanelId.A to Rect(40, 0, 20, 12)))

        assertEquals(SetPanelId.MAIN, set.hitTest(0, 0)?.id)
        assertNull(set.hitTest(0, 0)?.contentPoint)
        assertEquals(SetPanelId.A, set.hitTest(40, 0)?.id)
    }

    @Test
    fun `panels rendered into an offset canvas take and publish frame coordinates`() {
        val set = PanelSet(listOf(mainPanel(), sidePanel(SetPanelId.A)))
        val body = Rect(5, 2, 55, 10)
        val (main, side) = Layout.horizontal(Constraint.Fill(1), Constraint.Length(20)).split(body)

        Panels<SetPanelId, Unit>(Unit, mapOf(SetPanelId.MAIN to main, SetPanelId.A to side))
            .render(Canvas.of(Buffer(60, 12)).region(body), set)

        val layout = checkNotNull(set.layout)
        assertEquals(Rect(5, 2, 35, 10), checkNotNull(layout.slotOf(SetPanelId.MAIN)).outer)
        val slot = checkNotNull(layout.slotOf(SetPanelId.A))
        assertEquals(Rect(40, 2, 20, 10), slot.outer)
        assertNull(set.hitTest(0, 0))
        assertEquals(Point(0, 0), set.hitTest(slot.content.x, slot.content.y + 1)?.contentPoint)
    }

    private fun stubView(lines: Int = 40): MeasurableWidget = MeasurableWidget { writer ->
        repeat(lines) { row -> writer.writeLine("row$row") }
    }

    private fun mainPanel() = Panel<SetPanelId, Unit>(
        id = SetPanelId.MAIN,
        title = "MAIN",
        normal = { stubView() },
    )

    private fun sidePanel(id: SetPanelId, builds: (() -> Unit)? = null) = Panel<SetPanelId, Unit>(
        id = id,
        title = id.name,
        normal = {
            builds?.invoke()
            stubView()
        },
        minimized = { stubView(1) },
        maximized = { stubView() },
    )

    /** Renders [visible] as equal columns, in declaration order, across a [width]x[height] frame. */
    private fun render(
        set: PanelSet<SetPanelId, Unit>,
        visible: Set<SetPanelId>,
        width: Int = 80,
        height: Int = 24,
    ): PanelLayout<SetPanelId> {
        val ids = SetPanelId.entries.filter { it in visible }
        val columns = Layout.horizontal(ids.map { Constraint.Fill(1) }).split(Rect(0, 0, width, height))
        return renderAreas(set, ids.zip(columns).toMap(), width, height)
    }

    private fun renderAreas(
        set: PanelSet<SetPanelId, Unit>,
        areas: Map<SetPanelId, Rect>,
        width: Int = 80,
        height: Int = 24,
    ): PanelLayout<SetPanelId> {
        Panels(Unit, areas).render(Canvas.of(Buffer(width, height)), set)
        return checkNotNull(set.layout)
    }

    private fun PanelLayout<SetPanelId>.offsetOf(id: SetPanelId): Point =
        checkNotNull(checkNotNull(slotOf(id)).scroll).offset
}
