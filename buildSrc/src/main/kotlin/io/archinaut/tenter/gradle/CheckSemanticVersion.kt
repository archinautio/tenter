package io.archinaut.tenter.gradle

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations
import javax.inject.Inject

/**
 * Fails when the version in [versionFile] is lower than semantic versioning allows after the
 * latest release tag, given how the ABI dump changed since then.
 */
@UntrackedTask(because = "It checks against the repository history")
abstract class CheckSemanticVersion @Inject constructor(exec: ExecOperations) : ApiDumpTask(exec) {
    @get:Internal
    abstract val versionFile: RegularFileProperty

    @TaskAction
    fun check() {
        val release = SemanticVersion.parse(readProjectVersion(versionFile.get().asFile))
        val previousRelease = history().previousRelease()
        if (previousRelease == null) {
            logger.quiet("No previous release tag: $release needs no semantic version check.")
            return
        }
        val change = apiDiffSince(previousRelease).change
        val lowest = change.lowestReleaseAfter(SemanticVersion.parse(previousRelease))
        if (release < lowest) {
            throw GradleException(
                "Version $release is too low: the public API has ${change.description} since " +
                    "$previousRelease, so semantic versioning needs at least $lowest. " +
                    "Release it with -Prelease.releaseVersion=$lowest or higher.",
            )
        }
        logger.quiet("Version $release fits the public API since $previousRelease, which has ${change.description}.")
    }
}
