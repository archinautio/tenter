import io.archinaut.tenter.gradle.ApiChangesReport
import io.archinaut.tenter.gradle.ApiDumpTask
import io.archinaut.tenter.gradle.CheckDocsVersion
import io.archinaut.tenter.gradle.CheckSemanticVersion
import io.archinaut.tenter.gradle.DocsVersionTask
import io.archinaut.tenter.gradle.SemanticVersion
import io.archinaut.tenter.gradle.SyncDocsVersion
import java.util.regex.Matcher

plugins {
    base
    alias(libs.plugins.release)
}

tasks.named("build") {
    dependsOn(":tenter:build", ":tenter-example:build")
}

fun DocsVersionTask.useProjectFiles() {
    versionFile.set(layout.projectDirectory.file("gradle.properties"))
    docsFile.set(layout.projectDirectory.file("docs/site/index.md"))
}

val checkDocsVersion = tasks.register<CheckDocsVersion>("checkDocsVersion") {
    group = "verification"
    description = "Checks that the getting-started guide shows the version in gradle.properties."
    useProjectFiles()
}
tasks.named("check") { dependsOn(checkDocsVersion) }

tasks.register<SyncDocsVersion>("syncDocsVersion") {
    description = "Rewrites the getting-started guide to show the version in gradle.properties."
    useProjectFiles()
}

fun ApiDumpTask.useLibraryDump() {
    repository.set(layout.projectDirectory)
    apiDump.set("tenter/api/tenter.api")
}

val reportApiChanges = tasks.register<ApiChangesReport>("reportApiChanges") {
    group = "help"
    description = "Shows how tenter/api/tenter.api changed since the latest release tag."
    useLibraryDump()
}

val checkSemanticVersion = tasks.register<CheckSemanticVersion>("checkSemanticVersion") {
    group = "verification"
    description = "Checks that the version is high enough for the API changes since the latest release tag."
    useLibraryDump()
    versionFile.set(layout.projectDirectory.file("gradle.properties"))
    // In a release, it checks the confirmed version before anything is built or committed.
    mustRunAfter("unSnapshotVersion", "confirmReleaseVersion")
}

release {
    tagTemplate.set("v\${version}")
    preTagCommitMessage.set("chore(release): release")
    tagCommitMessage.set("chore(release): release")
    newVersionCommitMessage.set("chore(release): start")
    buildTasks.set(listOf(":build", ":tenter-example:packagedSmoke"))
    // The next development version raises the minor version: 0.2.0 is followed by 0.3.0-SNAPSHOT.
    versionPatterns = mapOf(
        """^(\d+\.\d+\.\d+)(.*)$""" to KotlinClosure2<Matcher, Project, String>({ matcher, _ ->
            "${SemanticVersion.parse(matcher.group(1)).nextMinor()}${matcher.group(2)}"
        }),
    )
    git {
        requireBranch.set("main")
        pushToRemote.set(false)
        // Both version commits also carry the docs synced below.
        commitVersionFileOnly.set(false)
    }
}

// Shows the API changes right before the release version is confirmed.
tasks.named("confirmReleaseVersion") { dependsOn(reportApiChanges) }

// A task runs once per build, so each version change in a release gets its own docs sync.
val syncDocsReleaseVersion = tasks.register<SyncDocsVersion>("syncDocsReleaseVersion") {
    useProjectFiles()
    mustRunAfter("unSnapshotVersion", "confirmReleaseVersion", checkSemanticVersion)
}
tasks.named("runBuildTasks") { dependsOn(checkSemanticVersion, syncDocsReleaseVersion) }

val syncDocsNextVersion = tasks.register<SyncDocsVersion>("syncDocsNextVersion") {
    useProjectFiles()
    mustRunAfter("updateVersion")
}
tasks.named("commitNewVersion") { dependsOn(syncDocsNextVersion) }

// Installs the release artifacts while the version is still the release one; publishing to
// Maven Central will hook in here too.
tasks.named("afterReleaseBuild") {
    dependsOn(":tenter:publishToMavenLocal")
}
