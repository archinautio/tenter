package io.archinaut.tenter.gradle

/** How an ABI dump changed between two revisions, read from its unified diff. */
class ApiDiff(val text: String) {
    private val changedLines = text.lines().filterNot { it.startsWith("+++") || it.startsWith("---") }

    val removed: Int = changedLines.count { it.startsWith("-") }
    val added: Int = changedLines.count { it.startsWith("+") }

    // A changed declaration shows as a removed line plus an added one, so any removal breaks callers.
    val change: ApiChange = when {
        removed > 0 -> ApiChange.Breaking
        added > 0 -> ApiChange.Additive
        else -> ApiChange.Unchanged
    }

    /** The diff without its file headers, starting at the first `@@` hunk. */
    val hunks: String = text.lines().dropWhile { !it.startsWith("@@") }.joinToString("\n")
}

/** The kind of public API change, and the lowest release semantic versioning allows after it. */
sealed interface ApiChange {
    val description: String

    /** Before 1.0 the minor version marks breaking changes, and the patch version everything else. */
    fun lowestReleaseAfter(previous: SemanticVersion): SemanticVersion

    /** One sentence for the release notes about the change since [previousRelease]. */
    fun summarySince(previousRelease: String): String

    data object Breaking : ApiChange {
        override val description = "removed or changed declarations"

        override fun lowestReleaseAfter(previous: SemanticVersion): SemanticVersion =
            if (previous.major == 0) previous.nextMinor() else previous.nextMajor()

        override fun summarySince(previousRelease: String): String =
            "The public API has removed or changed declarations since $previousRelease, so this release breaks callers."
    }

    data object Additive : ApiChange {
        override val description = "only added declarations"

        override fun lowestReleaseAfter(previous: SemanticVersion): SemanticVersion =
            if (previous.major == 0) previous.nextPatch() else previous.nextMinor()

        override fun summarySince(previousRelease: String): String =
            "The public API only adds declarations since $previousRelease."
    }

    data object Unchanged : ApiChange {
        override val description = "no changes"

        override fun lowestReleaseAfter(previous: SemanticVersion): SemanticVersion = previous.nextPatch()

        override fun summarySince(previousRelease: String): String =
            "The public API is unchanged since $previousRelease."
    }
}
