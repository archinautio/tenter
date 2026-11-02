// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.pages

import com.github.ajalt.mordant.input.KeyboardEvent
import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.input.HintGroup
import io.archinaut.tenter.input.KeyBinding
import io.archinaut.tenter.input.KeyLayer
import io.archinaut.tenter.input.KeySection
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.widget.FlashMessage
import io.archinaut.tenter.widget.Gauge
import io.archinaut.tenter.widget.Stack
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import tenterexample.pages.PageSections.heading
import tenterexample.pages.PageSections.note

/** Gauges at a ratio the user sets, a timed task, and flash messages the shell shows in its status bar. */
internal class ProgressPage : ShowcasePage<ProgressPage.State, ProgressPage.Action> {

    /** A [message] flashed at [at], on the page's clock. */
    internal data class Flash(
        val message: FlashMessage,
        val at: Duration,
    )

    internal data class State(
        val percent: Int = 40,
        val flash: Flash? = null,
        val taskStartedAt: Duration? = null,
    )

    internal enum class Action { LESS, MORE, FLASH, START_TASK }

    override val title: String = "Progress"

    override val initialState: State = State()

    private val keys = PageKeys(
        KeyLayer(
            title = "PROGRESS",
            bindings = listOf(
                KeyBinding(KeyboardEvent("ArrowLeft"), Action.LESS, "ratio"),
                KeyBinding(KeyboardEvent("ArrowRight"), Action.MORE, "ratio"),
                KeyBinding(KeyboardEvent("f"), Action.FLASH, "flash"),
                KeyBinding(KeyboardEvent("s"), Action.START_TASK, "task"),
            ),
            hintGroups = listOf(
                HintGroup("ratio", "←/→", "lower or raise the gauges' ratio"),
                HintGroup("flash", "f", "flash a message in the status bar"),
                HintGroup("task", "s", "start a timed task"),
            ),
        ),
    )

    override fun resolve(state: State, event: KeyboardEvent): Action? = keys.resolve(event)

    override fun hints(state: State): List<KeySection> = keys.hints

    override fun handle(state: State, action: Action, now: Duration): State = when (action) {
        Action.LESS -> state.copy(percent = (state.percent - PERCENT_STEP).coerceAtLeast(0))
        Action.MORE -> state.copy(percent = (state.percent + PERCENT_STEP).coerceAtMost(100))
        Action.FLASH -> state.copy(flash = Flash(FlashMessage("Flash messages vanish after three seconds"), now))
        Action.START_TASK -> if (isRunning(taskProgress(state, now))) state else state.copy(taskStartedAt = now)
    }

    override fun view(state: State, now: Duration): PageView {
        val ratio = state.percent / 100.0
        val task = taskProgress(state, now)
        val taskFlash = state.taskStartedAt
            ?.takeIf { task == 1.0 }
            ?.let { Flash(FlashMessage("The task finished", style = Style(StandardRole.SUCCESS)), it + TASK_DURATION) }
        val flash = listOfNotNull(state.flash, taskFlash).filter { now < it.at + it.message.duration }.maxByOrNull { it.at }
        val taskLabel = when (task) {
            null -> "idle"
            1.0 -> "done"
            else -> "${(task * 100).toInt()}%".padStart(4)
        }
        return PageView(
            Stack(
                listOf(
                    heading("Gauge at ${state.percent}%"),
                    Stack(
                        listOf(
                            Gauge(ratio),
                            Gauge(ratio, label = "disk"),
                            Gauge(ratio, label = "load", filledStyle = Style(thresholdRole(ratio))),
                        ),
                        spacing = 0,
                    ),
                    heading("A timed task"),
                    Gauge(task ?: 0.0, label = taskLabel, filledStyle = Style(StandardRole.SUCCESS)),
                    heading("FlashMessage"),
                    note(
                        "The status bar shows a page's flash message; the application dismisses it once " +
                            "its duration has passed. The task flashes one when it finishes.",
                    ),
                ),
            ),
            nextChangeIn = listOfNotNull(
                TASK_STEP.takeIf { isRunning(task) },
                flash?.let { it.at + it.message.duration - now },
            ).minOrNull(),
            flash = flash?.message,
        )
    }

    /** How far the task has run, from 0 to 1, or null before it first starts. */
    private fun taskProgress(state: State, now: Duration): Double? =
        state.taskStartedAt?.let { ((now - it) / TASK_DURATION).coerceIn(0.0, 1.0) }

    private fun isRunning(task: Double?): Boolean = task != null && task < 1.0

    /** The load gauge's color, warning and then danger as it fills. */
    private fun thresholdRole(ratio: Double): StandardRole = when {
        ratio >= DANGER_RATIO -> StandardRole.DANGER
        ratio >= WARNING_RATIO -> StandardRole.WARNING
        else -> StandardRole.SUCCESS
    }

    private companion object {
        private const val PERCENT_STEP: Int = 5
        private const val WARNING_RATIO: Double = 0.7
        private const val DANGER_RATIO: Double = 0.9
        private val TASK_DURATION: Duration = 4.seconds
        private val TASK_STEP: Duration = 100.milliseconds
    }
}
