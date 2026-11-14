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
}

/** The kind of public API change, and the lowest release semantic versioning allows after it. */
sealed interface ApiChange {
    val description: String

    /** Before 1.0 the minor version marks breaking changes, and the patch version everything else. */
    fun lowestReleaseAfter(previous: SemanticVersion): SemanticVersion

    data object Breaking : ApiChange {
        override val description = "removed or changed declarations"

        override fun lowestReleaseAfter(previous: SemanticVersion): SemanticVersion =
            if (previous.major == 0) previous.nextMinor() else previous.nextMajor()
    }

    data object Additive : ApiChange {
        override val description = "only added declarations"

        override fun lowestReleaseAfter(previous: SemanticVersion): SemanticVersion =
            if (previous.major == 0) previous.nextPatch() else previous.nextMinor()
    }

    data object Unchanged : ApiChange {
        override val description = "no changes"

        override fun lowestReleaseAfter(previous: SemanticVersion): SemanticVersion = previous.nextPatch()
    }
}
