import org.spongepowered.configurate.build.applyCommonAttributes

plugins {
    kotlin("jvm") version "1.4.10" apply false
    id("org.jlleitschuh.gradle.ktlint") version "9.4.1"
    id("io.freefair.aggregate-javadoc-jar") version "5.2.1"
    id("org.ajoberstar.grgit")
    id("org.ajoberstar.git-publish") version "3.0.0"
    id("com.github.ben-manes.versions") version "0.33.0"
    id("io.codearte.nexus-staging")
    `java-base`
}

group = "org.spongepowered"
version = "4.0.0-SNAPSHOT"

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}

allprojects {
    repositories {
        jcenter()
    }

    ktlint {
        version.set("0.39.0")
    }
}

nexusStaging {
    val sonatypeUsername: String? by project
    val sonatypePassword: String? by project
    username = sonatypeUsername
    password = sonatypePassword
}

tasks.aggregateJavadoc.configure {
    val gradleJdk = JavaVersion.current()
    // at least java 11, but not 12 (java 12 is broken for some reason :( )
    if (gradleJdk < JavaVersion.VERSION_11 || gradleJdk == JavaVersion.VERSION_12) {
        javadocTool.set(javaToolchains.javadocToolFor { this.languageVersion.set(JavaLanguageVersion.of(11)) })
    }

    applyCommonAttributes()
    title = "Configurate $version (all modules)"

    val excludedProjects = listOf("examples").map {
        project(":$it").tasks.named("javadoc", Javadoc::class).get().classpath
    }
    exclude {
        excludedProjects.find { coll -> coll.contains(it.file) } != null
    }

    (options as? StandardJavadocDocletOptions)?.apply {
        addBooleanOption("Xdoclint:-missing", true)
        encoding = "UTF-8"
        source = "1.8"
    }
}

gitPublish {
    branch.set("gh-pages")
    contents {
        from("src/site") {
            val versions = {
                (listOf(project.version as String) + grgit.tag.list().map { it.name }.reversed())
                    .filter { repoDir.get().dir(it).getAsFile().exists() || it == project.version }
            }
            expand("project" to project, "versions" to versions)
        }
        from(tasks.aggregateJavadoc) {
            into("$version/apidocs")
            if (!(version as String).endsWith("SNAPSHOT")) {
                into("apidocs")
            }
        }
    }

    preserve {
        include(".gitattributes")
        include("**/") // include everything in directories
        exclude("/*.html")
    }

    commitMessage.set("Publish javadocs (via gradle-git-publish)")
}

tasks.dependencyUpdates {
    gradleReleaseChannel = "current"
    revision = "release"
}
