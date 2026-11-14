package io.archinaut.tenter.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations
import javax.inject.Inject

/** The ABI dump at [apiDump], inside the git repository at [repository]. */
abstract class ApiDumpTask @Inject constructor(private val exec: ExecOperations) : DefaultTask() {
    @get:Internal
    abstract val repository: DirectoryProperty

    /** Path of the dump, relative to [repository]. */
    @get:Internal
    abstract val apiDump: Property<String>

    protected fun history(): ReleaseHistory = ReleaseHistory(exec, repository.get().asFile)

    protected fun apiDiffSince(tag: String): ApiDiff = ApiDiff(history().diffSince(tag, apiDump.get()))
}

/**
 * Logs how the ABI dump changed since the previous release tag, so a release can be judged.
 * With [releaseNotes] set, it also writes that change as the release notes' Public API section.
 */
@UntrackedTask(because = "It reports against the repository history")
abstract class ApiChangesReport @Inject constructor(exec: ExecOperations) : ApiDumpTask(exec) {
    @get:Internal
    abstract val releaseNotes: RegularFileProperty

    @TaskAction
    fun report() {
        val previousRelease = history().previousRelease()
        val diff = previousRelease?.let(::apiDiffSince)
        logger.quiet(consoleReport(previousRelease, diff))
        if (releaseNotes.isPresent) {
            val file = releaseNotes.get().asFile
            file.parentFile.mkdirs()
            file.writeText(releaseNotesSection(previousRelease, diff))
        }
    }

    private fun consoleReport(previousRelease: String?, diff: ApiDiff?): String = when {
        previousRelease == null || diff == null ->
            "No previous release tag: the whole of ${apiDump.get()} is new API."
        diff.change == ApiChange.Unchanged -> "Public API unchanged since $previousRelease."
        diff.change == ApiChange.Breaking -> "Public API since $previousRelease: ${diff.removed} lines removed or " +
            "changed, ${diff.added} added. Removals break callers.\n${diff.text}"
        else -> "Public API since $previousRelease: ${diff.added} lines added, none removed.\n${diff.text}"
    }

    private fun releaseNotesSection(previousRelease: String?, diff: ApiDiff?): String = buildString {
        appendLine("### Public API 🔍")
        appendLine()
        if (previousRelease == null || diff == null) {
            appendLine("First release: no earlier API to compare with.")
            return@buildString
        }
        appendLine(diff.change.summarySince(previousRelease))
        if (diff.change == ApiChange.Unchanged) return@buildString
        appendLine()
        appendLine("<details>")
        appendLine("<summary>Changes to <code>${apiDump.get()}</code></summary>")
        appendLine()
        appendLine("```diff")
        appendLine(diff.hunks)
        appendLine("```")
        appendLine()
        appendLine("</details>")
    }
}
