package io.archinaut.tenter.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
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

/** Logs how the ABI dump changed since the latest release tag, so a release can be judged. */
@UntrackedTask(because = "It reports against the repository history")
abstract class ApiChangesReport @Inject constructor(exec: ExecOperations) : ApiDumpTask(exec) {
    @TaskAction
    fun report() {
        val previousRelease = history().previousRelease()
        if (previousRelease == null) {
            logger.quiet("No previous release tag: the whole of ${apiDump.get()} is new API.")
            return
        }
        val diff = apiDiffSince(previousRelease)
        logger.quiet(
            when (diff.change) {
                ApiChange.Unchanged -> "Public API unchanged since $previousRelease."
                ApiChange.Breaking -> "Public API since $previousRelease: ${diff.removed} lines removed or " +
                    "changed, ${diff.added} added. Removals break callers.\n${diff.text}"
                ApiChange.Additive ->
                    "Public API since $previousRelease: ${diff.added} lines added, none removed.\n${diff.text}"
            },
        )
    }
}
