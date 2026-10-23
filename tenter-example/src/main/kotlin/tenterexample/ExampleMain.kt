// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample

import com.github.ajalt.mordant.input.MouseEvent
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking
import io.archinaut.tenter.widget.panel.PanelMode
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Padding
import io.archinaut.tenter.terminal.Terminal
import io.archinaut.tenter.terminal.TerminalEvent
import io.archinaut.tenter.terminal.withScreen
import io.archinaut.tenter.widget.Block
import io.archinaut.tenter.widget.Widget
import io.archinaut.tenter.widget.MeasurableWidget
import io.archinaut.tenter.widget.ViewportState
import io.archinaut.tenter.widget.Viewport
import io.archinaut.tenter.widget.block
import io.archinaut.tenter.widget.measure

/**
 * Runs the separate-consumer smoke scenario without raw input or terminal lifecycle escapes.
 * It exercises long measured content, composition, settled hit-testing, an application-owned panel layout,
 * widget glyphs, keymap dispatch, scrolling, reveal following, panel states, and animation.
 */
public fun runHeadlessSmoke(): SmokeResult {
    // --8<-- [start:padding]
    val framed = MeasurableWidget { writer ->
        writer.writeLine("OK")
        writer.markReveal(row = 0)
    }.block(Block.bordered(padding = Padding.uniform(1)))
    val size = framed.measure(8)
    // --8<-- [end:padding]
    val framedCanvas = Canvas.offscreen(size.width, size.height)
    framed.render(framedCanvas)
    check(size.height == 5 && framedCanvas.get(2, 2).symbol == "O")
    val followed = ViewportState()
    Viewport(framed).render(Canvas.offscreen(size.width, 3), followed)
    check(followed.settled?.revealed?.y == 2) { "the block lost the child's reveal" }

    val app = ExampleApp()
    val terminal = Terminal.headless(width = 140, height = 24)
    var frame = terminal.draw(app::render)
    val initialText = frame.lines().joinToString("\n")
    check("row 0" in initialText) { "the rendered list did not contain its first generated row" }
    check("PgUp/PgDn" in initialText) { "the rendered help did not contain the keymap hints" }

    val initialLayout = checkNotNull(app.panels.layout)
    val wheelTarget = checkNotNull(initialLayout.slotOf(ExamplePanelId.ROWS)).content
    repeat(EXAMPLE_ROW_COUNT) {
        app.handle(MouseEvent(x = wheelTarget.x + 1, y = wheelTarget.y + 1, wheelDown = true))
    }
    frame = terminal.draw(app::render)
    val scrolledText = frame.lines().joinToString("\n")
    check("row 599" in scrolledText) { "wheel scrolling did not reach the final generated row" }

    val scrolledLayout = checkNotNull(app.panels.layout)
    val clickTarget = checkNotNull(scrolledLayout.slotOf(ExamplePanelId.ROWS)).content
    val clickX = clickTarget.x + 1
    val clickY = clickTarget.y + 1
    val hit = checkNotNull(app.panels.hitTest(clickX, clickY))
    val clickedRow = checkNotNull(hit.contentPoint).y
    app.handle(MouseEvent(x = clickX, y = clickY, left = true))
    terminal.resize(44, 8)
    frame = terminal.draw(app::render)
    check(app.state.selectedRow == clickedRow) { "the click selected the wrong generated row" }
    check(app.state.selectedRow in app.state.checkedRows) { "the click did not toggle the selected row" }
    check(frame.width == 44 && frame.height == 8) { "the resized frame did not use the new dimensions" }

    checkPanelLayout()

    val animation = animationProbe()
    val firstFrame = renderAnimationFrame(animation.firstFrame)
    val secondFrame = renderAnimationFrame(animation.secondFrame)
    check(firstFrame.get(1, 0).symbol == "A") { "the first animation frame painted the wrong glyph" }
    check(secondFrame.get(1, 0).symbol == "B") { "the second animation frame painted the wrong glyph" }

    return SmokeResult(
        renderedRows = EXAMPLE_ROW_COUNT,
        helpContainsMovement = "PgUp/PgDn" in initialText,
        animationCompleted = animation.completedFrameCount == 0,
    )
}

/** Interactive entry point; run the installed distribution's launcher from a real terminal. */
public fun main(args: Array<String>) {
    if ("--hello" in args) {
        tenterexample.hello.main()
        return
    }
    if ("--headless" in args) {
        println(runHeadlessSmoke())
        return
    }

    Terminal().withScreen { terminal -> runInteractive(terminal) }
}

/**
 * Checks the application-owned layout: the status bar on top, the catalog filling the left, the
 * side panels at their mode's width on the right, and a maximized panel taking the whole body.
 */
private fun checkPanelLayout() {
    val app = ExampleApp()
    val terminal = Terminal.headless(width = 140, height = 24)

    terminal.draw(app::render)
    val normal = checkNotNull(app.panels.layout)
    val rows = checkNotNull(normal.slotOf(ExamplePanelId.ROWS)).outer
    val details = checkNotNull(normal.slotOf(ExamplePanelId.DETAILS)).outer
    val help = checkNotNull(normal.slotOf(ExamplePanelId.HELP)).outer
    check(rows.x == 0 && rows.y == ExampleApp.STATUS_HEIGHT) { "the catalog is not below the status bar at the left" }
    check(details.x == rows.width && help.x + help.width == 140) { "the side panels are not right of the catalog" }

    check(app.cycle(ExamplePanelId.DETAILS) == PanelMode.MAXIMIZED) { "the details panel did not maximize" }
    terminal.draw(app::render)
    val maximized = checkNotNull(app.panels.layout).slots.single()
    check(maximized.id == ExamplePanelId.DETAILS && maximized.outer.width == 140) { "a maximized panel did not take the body" }

    check(app.cycle(ExamplePanelId.DETAILS) == PanelMode.MINIMIZED) { "the details panel did not minimize" }
    terminal.draw(app::render)
    val minimized = checkNotNull(checkNotNull(app.panels.layout).slotOf(ExamplePanelId.DETAILS)).outer
    check(minimized.width == ExampleApp.MINIMIZED_WIDTH) { "a minimized panel did not shrink to its stub" }
    check(app.cycle(ExamplePanelId.DETAILS) == PanelMode.NORMAL) { "the details panel did not restore its normal state" }

    check(app.cycle(ExamplePanelId.ROWS) == PanelMode.MAXIMIZED) { "the catalog did not maximize" }
    check(app.cycle(ExamplePanelId.ROWS) == PanelMode.NORMAL) { "the catalog has a mode besides normal and maximized" }
    check(app.cycle(ExamplePanelId.HELP) == PanelMode.MAXIMIZED) { "the help panel did not maximize" }
    check(app.cycle(ExamplePanelId.HELP) == PanelMode.NORMAL) { "the help panel has a mode besides normal and maximized" }
}

private fun runInteractive(terminal: Terminal) {
    val app = ExampleApp()
    terminal.draw(app::render)

    // --8<-- [start:input-loop]
    runBlocking {
        terminal.events()
            // A Resized event needs no handling: the next draw follows the backend's new size.
            .onEach { event -> if (event is TerminalEvent.Input) app.handle(event.event) }
            // The quit key sets running to false; stopping collection releases the terminal.
            .takeWhile { app.running }
            .collect { terminal.draw(app::render) }
    }
    // --8<-- [end:input-loop]
}

private fun renderAnimationFrame(widget: Widget): Canvas {
    val canvas = Canvas.offscreen(3, 1)
    widget.render(canvas)
    return canvas
}
