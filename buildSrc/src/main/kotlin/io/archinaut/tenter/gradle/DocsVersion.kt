package io.archinaut.tenter.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations
import javax.inject.Inject

/** Each place in the docs that shows the library version, matching exactly that version. */
private val versionMentions = listOf(
    Regex("""(?<=latest release, `)[^`]+(?=`)"""),
    Regex("""(?<="io\.archinaut:tenter:)[^"]+(?=")"""),
)

/**
 * The docs page in [docsFile] that shows the latest released version, which readers can resolve
 * from Maven Central. That is the version in [versionFile] while a release is being made, and
 * the latest `v*` tag of [repository] while it holds a snapshot.
 */
@UntrackedTask(because = "It reads the repository tags")
abstract class DocsVersionTask @Inject constructor(private val exec: ExecOperations) : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val versionFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val docsFile: RegularFileProperty

    @get:Internal
    abstract val repository: DirectoryProperty

    protected fun releasedVersion(): String {
        val version = readProjectVersion(versionFile.get().asFile)
        if (!version.endsWith("-SNAPSHOT")) return version
        val tag = ReleaseHistory(exec, repository.get().asFile).latestRelease()
            ?: throw GradleException("No v* tag before $version, so the docs have no released version to show")
        return tag.removePrefix("v")
    }

    protected fun docsText(): String {
        val text = docsFile.get().asFile.readText()
        val missing = versionMentions.filter { it.find(text) == null }
        if (missing.isNotEmpty()) {
            throw GradleException("${docsFile.get()} no longer mentions the version where expected: $missing")
        }
        return text
    }
}

/** Rewrites every version mention in the docs page to the latest released version. */
abstract class SyncDocsVersion @Inject constructor(exec: ExecOperations) : DocsVersionTask(exec) {
    @TaskAction
    fun sync() {
        val version = releasedVersion()
        val synced = versionMentions.fold(docsText()) { text, mention -> mention.replace(text, version) }
        docsFile.get().asFile.writeText(synced)
    }
}

/** Fails when a version mention in the docs page differs from the latest released version. */
abstract class CheckDocsVersion @Inject constructor(exec: ExecOperations) : DocsVersionTask(exec) {
    @TaskAction
    fun check() {
        val version = releasedVersion()
        val text = docsText()
        val stale = versionMentions.flatMap { it.findAll(text) }.map { it.value }.filter { it != version }
        if (stale.isNotEmpty()) {
            throw GradleException(
                "${docsFile.get()} shows version ${stale.distinct()} but the latest release is $version. " +
                    "Run ./gradlew syncDocsVersion",
            )
        }
    }
}
