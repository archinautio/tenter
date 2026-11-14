package io.archinaut.tenter.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

/** Each place in the docs that shows the library version, matching exactly that version. */
private val versionMentions = listOf(
    Regex("""(?<=currently `)[^`]+(?=`)"""),
    Regex("""(?<="io\.archinaut:tenter:)[^"]+(?=")"""),
)

/** The library version in [versionFile], and the docs page in [docsFile] that mentions it. */
abstract class DocsVersionTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val versionFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val docsFile: RegularFileProperty

    protected fun projectVersion(): String = readProjectVersion(versionFile.get().asFile)

    protected fun docsText(): String {
        val text = docsFile.get().asFile.readText()
        val missing = versionMentions.filter { it.find(text) == null }
        if (missing.isNotEmpty()) {
            throw GradleException("${docsFile.get()} no longer mentions the version where expected: $missing")
        }
        return text
    }
}

/** Rewrites every version mention in the docs page to the project version. */
@UntrackedTask(because = "It rewrites its own input")
abstract class SyncDocsVersion : DocsVersionTask() {
    @TaskAction
    fun sync() {
        val version = projectVersion()
        val synced = versionMentions.fold(docsText()) { text, mention -> mention.replace(text, version) }
        docsFile.get().asFile.writeText(synced)
    }
}

/** Fails when a version mention in the docs page differs from the project version. */
abstract class CheckDocsVersion : DocsVersionTask() {
    @TaskAction
    fun check() {
        val version = projectVersion()
        val text = docsText()
        val stale = versionMentions.flatMap { it.findAll(text) }.map { it.value }.filter { it != version }
        if (stale.isNotEmpty()) {
            throw GradleException(
                "${docsFile.get()} shows version ${stale.distinct()} but the project is $version. " +
                    "Run ./gradlew syncDocsVersion",
            )
        }
    }
}
