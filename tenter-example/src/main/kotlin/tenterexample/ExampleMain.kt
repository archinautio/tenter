// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample

import com.github.ajalt.mordant.input.MouseEvent
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.TerminalRecorder
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking
import io.archinaut.tenter.palette.DefaultRolePalette
import io.archinaut.tenter.widget.panel.PanelMode
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Padding
import io.archinaut.tenter.terminal.CompletedFrame
import io.archinaut.tenter.terminal.Terminal
import io.archinaut.tenter.terminal.TerminalEvent
import io.archinaut.tenter.terminal.events
import io.archinaut.tenter.terminal.withScreen
import io.archinaut.tenter.widget.Block
import io.archinaut.tenter.widget.Widget
import io.archinaut.tenter.widget.MeasurableWidget
import io.archinaut.tenter.widget.ViewportState
import io.archinaut.tenter.widget.Viewport
import io.archinaut.tenter.widget.block
import io.archinaut.tenter.widget.measure
import com.github.ajalt.mordant.terminal.Terminal as MordantTerminal

/**
 * Runs the separate-consumer smoke scenario without raw input or terminal lifecycle escapes.
 * It exercises long measured content, composition, settled hit-testing, both layout factories,
 * widget glyphs, keymap dispatch, scrolling, reveal following, panel states, and animation.
 */
public fun runHeadlessSmoke(): SmokeResult {
    // --8<-- [start:padding]
    val framed = MeasurableWidget { cursor ->
        cursor.writeLine("OK")
        cursor.markReveal(row = 0)
    }.block(Block.bordered(padding = Padding.uniform(1)))
    val size = framed.measure(8)
    // --8<-- [end:padding]
    val framedCanvas = Canvas.offscreen(size.width, size.height)
    framed.render(framedCanvas)
    check(size.height == 5 && framedCanvas.get(2, 2).symbol == "O")
    val followed = ViewportState()
    Viewport(framed).render(Canvas.offscreen(size.width, 3), followed)
    check(followed.settled?.revealed?.y == 2) { "the block lost the child's reveal" }

    val app = ExampleApp(ExampleLayoutMode.MAIN_AND_SIDES)
    val terminal = headlessTerminal(width = 80, height = 12)
    var frame = terminal.draw(app::render)
    val initialText = frameText(frame)
    check("row 0" in initialText) { "the rendered list did not contain its first generated row" }
    check("PgUp/PgDn" in initialText) { "the rendered help did not contain the keymap hints" }

    val initialLayout = checkNotNull(app.panels.layout)
    val wheelTarget = checkNotNull(initialLayout.main).content
    repeat(EXAMPLE_ROW_COUNT) {
        app.handle(MouseEvent(x = wheelTarget.x + 1, y = wheelTarget.y + 1, wheelDown = true))
    }
    frame = terminal.draw(app::render)
    val scrolledText = frameText(frame)
    check("row 599" in scrolledText) { "wheel scrolling did not reach the final generated row" }

    val scrolledLayout = checkNotNull(app.panels.layout)
    val clickTarget = checkNotNull(scrolledLayout.main).content
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

    check(app.cycleHelp() == PanelMode.MAXIMIZED) { "the help panel did not maximize" }
    check(app.cycleHelp() == PanelMode.MINIMIZED) { "the help panel did not minimize" }
    check(app.cycleHelp() == PanelMode.NORMAL) { "the help panel did not restore its normal state" }

    val uniform = ExampleApp(ExampleLayoutMode.UNIFORM)
    headlessTerminal(width = 100, height = 12).draw(uniform::render)
    val uniformLayout = checkNotNull(uniform.panels.layout)
    check(uniformLayout.main == null && uniformLayout.sides.size == 2) {
        "the uniform layout did not place both fresh panel instances"
    }

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

    val backend = MordantTerminal()
    backend.withScreen(DefaultRolePalette) { terminal ->
        runInteractive(backend, terminal)
    }
}

private fun runInteractive(backend: MordantTerminal, terminal: Terminal) {
    val app = ExampleApp(ExampleLayoutMode.MAIN_AND_SIDES)
    terminal.draw(app::render)

    // --8<-- [start:input-loop]
    runBlocking {
        backend.events()
            // A Resized event needs no handling: the next draw follows the backend's new size.
            .onEach { event -> if (event is TerminalEvent.Input) app.handle(event.event) }
            // The quit key sets running to false; stopping collection releases the terminal.
            .takeWhile { app.running }
            .collect { terminal.draw(app::render) }
    }
    // --8<-- [end:input-loop]
}

/** A Tenter terminal over an in-memory recorder, for drawing frames without a real terminal. */
internal fun headlessTerminal(width: Int, height: Int): Terminal = Terminal(
    MordantTerminal(
        ansiLevel = AnsiLevel.NONE,
        width = width,
        height = height,
        terminalInterface = TerminalRecorder(ansiLevel = AnsiLevel.NONE, width = width, height = height),
    ),
    DefaultRolePalette,
)

private fun renderAnimationFrame(widget: Widget): Canvas {
    val canvas = Canvas.offscreen(3, 1)
    widget.render(canvas)
    return canvas
}

private fun frameText(frame: CompletedFrame): String =
    (0 until frame.height).joinToString("\n") { y ->
        (0 until frame.width).joinToString("") { x -> frame.get(x, y).symbol }
    }
