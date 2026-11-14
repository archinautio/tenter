package io.archinaut.tenter.gradle

import org.gradle.api.GradleException

/** A `MAJOR.MINOR.PATCH` version; a leading `v` and any suffix such as `-SNAPSHOT` are ignored. */
data class SemanticVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<SemanticVersion> {
    override fun compareTo(other: SemanticVersion): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })

    fun nextMajor(): SemanticVersion = SemanticVersion(major + 1, 0, 0)

    fun nextMinor(): SemanticVersion = SemanticVersion(major, minor + 1, 0)

    fun nextPatch(): SemanticVersion = SemanticVersion(major, minor, patch + 1)

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        private val pattern = Regex("""v?(\d+)\.(\d+)\.(\d+)\D?.*""")

        fun parse(text: String): SemanticVersion {
            val (major, minor, patch) = pattern.matchEntire(text)?.destructured
                ?: throw GradleException("'$text' is not a MAJOR.MINOR.PATCH version")
            return SemanticVersion(major.toInt(), minor.toInt(), patch.toInt())
        }
    }
}
