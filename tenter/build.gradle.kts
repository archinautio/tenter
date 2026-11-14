@file:OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)

import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm

plugins {
    id("tenter.kotlin-library")
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

group = "io.archinaut"

kotlin {
    abiValidation {
        referenceDumpDir.set(layout.projectDirectory.dir("api"))
    }
}

dependencies {
    api(libs.mordant)
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.konsist)
    testImplementation(libs.kotlinx.coroutines.test)
}

mavenPublishing {
    configure(KotlinJvm(javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml")))
    // Maven Central needs signed artifacts, but a release built without keys stays installable locally.
    val signingKey = providers.gradleProperty("signingInMemoryKey").orElse(providers.gradleProperty("signing.keyId"))
    if (signingKey.isPresent) signAllPublications()

    pom {
        val repositoryUrl = "https://github.com/archinautio/tenter"
        name.set("Tenter")
        description.set("A Kotlin/JVM terminal-UI toolkit built on Mordant.")
        url.set(repositoryUrl)
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("alejandropg")
                name.set("Alejandro Pérez García")
            }
        }
        scm {
            connection.set("scm:git:$repositoryUrl.git")
            developerConnection.set("scm:git:ssh://git@github.com/archinautio/tenter.git")
            url.set(repositoryUrl)
        }
    }
}
