package io.archinaut.tenter.gradle

import org.gradle.api.GradleException
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Properties

/** The release tags of the git repository at [repository], and how files changed since them. */
class ReleaseHistory(private val exec: ExecOperations, private val repository: File) {
    /** The latest `v*` tag reachable from `HEAD`, or null before the first release. */
    fun previousRelease(): String? = git("describe", "--tags", "--abbrev=0", "--match", "v*")

    fun diffSince(tag: String, path: String): String =
        git("diff", "--no-color", tag, "--", path) ?: throw GradleException("git diff $tag -- $path failed")

    private fun git(vararg arguments: String): String? {
        val output = ByteArrayOutputStream()
        val result = exec.exec {
            workingDir = repository
            commandLine("git", *arguments)
            standardOutput = output
            errorOutput = ByteArrayOutputStream()
            isIgnoreExitValue = true
        }
        return if (result.exitValue == 0) output.toString().trim() else null
    }
}

/** The `version` in a `gradle.properties` file, read when called because a release rewrites it. */
fun readProjectVersion(versionFile: File): String {
    val properties = Properties()
    versionFile.reader().use(properties::load)
    return properties.getProperty("version") ?: throw GradleException("No version in $versionFile")
}
