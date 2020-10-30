package org.spongepowered.configurate.build

import java.lang.Integer.max

plugins {
    `java-base`
}

val multireleaseExtension = extensions.create(MULTIRELEASE_EXTENSION, MultireleaseExtension::class)
val toolchains = extensions.getByType(JavaToolchainService::class)

val JavaVersion.intVersion: Int get() = this.ordinal + 1
val JavaVersion.releaseId: String get() = "java$majorVersion"

/**
 * Configure a toolchain spec based on the provided parameters' runtime versions,
 * and respecting the [strictVersion] parameter.
 */
fun JavaToolchainSpec.applyVersion(
    version: Provider<TargetVersion>,
    strictVersion: Provider<Boolean>,
    baseVersion: Provider<TargetVersion>,
    forExec: Boolean = false
) {
    this.languageVersion.set(version.flatMap { makeLanguageVersion(it, strictVersion, baseVersion, forExec) })
}

/**
 * Configure a toolchain spec based on the provided parameters' runtime versions,
 * and respecting the [strictVersion] parameter.
 */
fun JavaToolchainSpec.applyVersion(
    version: TargetVersion,
    strictVersion: Provider<Boolean>,
    baseVersion: Provider<TargetVersion>,
    forExec: Boolean = false
) {
    this.languageVersion.set(makeLanguageVersion(version, strictVersion, baseVersion, forExec))
}

/**
 * Create a language version provider for a certain target version.
 *
 * Unless marked as [forExec], the returned version will be at least the base
 * JVM version
 */
fun makeLanguageVersion(
    version: TargetVersion,
    strictVersion: Provider<Boolean>,
    baseVersion: Provider<TargetVersion>,
    forExec: Boolean
): Provider<JavaLanguageVersion> {
    val (_, toolchain) = version
    return strictVersion.map { strict ->
        JavaLanguageVersion.of(
            // Get the target version, making sure it's at least the base version
            max(
                if (!strict && toolchain <= JavaVersion.current()) {
                    // if we're non-strict, just require we're running at least the target version
                    JavaVersion.current().intVersion
                } else {
                    // otherwise, request the target version
                    toolchain.intVersion
                },
                // if we are executing, we want exact, so make sure the other branch of the max is higher
                if (forExec) 0 else baseVersion.get().jvm.intVersion
            )
        )
    }
}

// Prevent recursive initialization
val configuredSets = mutableSetOf<String>()

/** Register a source set with [name] and make sure it doesn't get configured recursively for multi-release */
fun SourceSetContainer.registerUnconfigurable(name: String, configure: SourceSet.() -> Unit): NamedDomainObjectProvider<SourceSet> {
    configuredSets += name
    return this.register(name, configure)
}

/**
 * Configure a single compile task based on our version spec
 */
fun JavaCompile.configureForRelease(version: TargetVersion, strictVersion: Provider<Boolean>, baseVersion: Provider<TargetVersion>) {
    val (release, toolchain) = version

    javaCompiler.set(toolchains.compilerFor { applyVersion(version, strictVersion, baseVersion) })

    // Configure source/target compatibility
    // sourceCompatibility = release.toString()
    // targetCompatibility = release.toString()
    if (toolchain.isJava9Compatible) {
        options.release.set(release.intVersion)
    }

    version.extraConfigureAction(this)
}

// step one: set up collections for main and test source sets

fun SourceSet.configureForVersions(versions: DomainObjectSet<TargetVersion>, baseVersion: Property<TargetVersion>) {
    if (name in configuredSets) return // we should not be configured
    val baseSet = this

    // Set the necessary attribute for multirelease jars
    val jarTask = if (jarTaskName in tasks.names) {
        tasks.named(jarTaskName, Jar::class) {
            manifest.attributes["Multi-Release"] = "true"
        }
    } else {
        null
    }

    val seenReleases = mutableSetOf<JavaVersion>()
    // Based on guidance at https://blog.gradle.org/mrjars
    // and an example at https://github.com/melix/mrjar-gradle
    // set up target version source sets, add to some collection to track
    // only configure toolchain
    versions.configureEach {
        // Basic assertions
        require(release !in seenReleases) { "Target java versions cannot be specified multiple times" }
        seenReleases.add(release)

        if (release == baseVersion.get().release) {
            return@configureEach
        }

        require(release >= JavaVersion.VERSION_1_9) { "Java versions before Java 9 cannot be in multirelease jars" }
        require(release > multireleaseExtension.baseVersion.get().release) {
            "All multirelease variants must be newer versions than the base release"
        }

        val spec = this
        val versionId = "java${spec.release.majorVersion}"
        sourceSets.registerUnconfigurable(getTaskName(versionId, null)) {
            java.srcDirs(project.projectDir.resolve("src/${baseSet.name}/$versionId"))
            // Depend on main source set
            project.dependencies.add(implementationConfigurationName, baseSet.output)

            // Set compatibility
            tasks.named(compileJavaTaskName, JavaCompile::class.java) {
                configureForRelease(spec, multireleaseExtension.strictVersions, multireleaseExtension.baseVersion)
            }

            // Add to output jar
            jarTask?.configure {
                into("META-INF/versions/${spec.release.majorVersion}") {
                    from(this@registerUnconfigurable.output)
                }
            }
        }
    }

    // are we a test source set? configure test tasks
    // how do we determine if we're a test set? flag?

    // then also configure main java compile task
    tasks.named(compileJavaTaskName, JavaCompile::class).configure {
        javaCompiler.set(toolchains.compilerFor { applyVersion(baseVersion, multireleaseExtension.strictVersions, baseVersion) })
        options.release.set(baseVersion.map { it.release.intVersion })
    }

    // how do we handle other JVM languages?
    // do other languages even matter?
}

sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).configure {
    configureForVersions(multireleaseExtension.targetVersions, multireleaseExtension.baseVersion)
}

val checkTask = tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME)
sourceSets.named(SourceSet.TEST_SOURCE_SET_NAME).configure {
    configureForVersions(multireleaseExtension.testTargetVersions, multireleaseExtension.baseVersion)

    // Create a new test execution task
    multireleaseExtension.testTargetVersions.configureEach version@{
        val versionId = release.releaseId
        // we're just changing the version of runtime
        if (release == multireleaseExtension.baseVersion.get().release) {
            tasks.named(JavaPlugin.TEST_TASK_NAME, Test::class) {
                javaLauncher.set(
                    toolchains.launcherFor {
                        applyVersion(this@version, multireleaseExtension.strictVersions, multireleaseExtension.baseVersion, forExec = true)
                    }
                )
            }

            // and skip the rest
            return@version
        }

        // Set up test task for this version
        // TODO: Only do this for when on strict versions, or we'd already get a JDK
        val versionedTestName = this@configure.getTaskName(versionId, null)
        sourceSets.named(versionedTestName).configure {
            val versionedTest = tasks.register(versionedTestName, Test::class.java) {
                onlyIf {
                    // strict versions or target is > current
                    multireleaseExtension.strictVersions.get() || jvm > JavaVersion.current()
                }

                // metadata setup
                group = LifecycleBasePlugin.VERIFICATION_GROUP
                description = "Execute tests on Java ${this@version.jvm.majorVersion}"
                // Set version to execute with
                javaLauncher.set(
                    toolchains.launcherFor {
                        applyVersion(this@version, multireleaseExtension.strictVersions, multireleaseExtension.baseVersion, forExec = true)
                    }
                )

                // Configure classpath
                testClassesDirs = output.classesDirs
                // classpath = runtimeClasspath // directly use the runtime classpath for the source set, it'll already include lower versions
            }
            checkTask.configure {
                dependsOn(versionedTest)
            }
        }
        // add source directory
    }
}

// configure Javadoc task
tasks.withType(Javadoc::class).configureEach {
    // Set Javadoc target version
    javadocTool.set(
        toolchains.javadocToolFor {
            applyVersion(multireleaseExtension.baseVersion, multireleaseExtension.strictVersions, multireleaseExtension.baseVersion)
        }
    )
}

// Then, after other things have a say, we can apply everything that's not a lazy property
afterEvaluate {
    // Apply JD source version
    tasks.withType(Javadoc::class).configureEach {
        // Configure source version of JD task
        options.source = multireleaseExtension.baseVersion.get().release.majorVersion
    }

    // Apply base version to test run task
    tasks.named(JavaPlugin.TEST_TASK_NAME, Test::class).configure {
        // Set version to execute with
        javaLauncher.set(
            toolchains.launcherFor {
                applyVersion(multireleaseExtension.baseVersion.get(), multireleaseExtension.strictVersions, multireleaseExtension.baseVersion)
            }
        )
    }

    // then, after we have finished up, and know we can get a sorted list of all versions
    // For all configured values in the source sets

    parent@ fun SourceSet.finalizeVersions(versions: DomainObjectSet<TargetVersion>, baseVersion: TargetVersion) {
        val sorted = versions.toSortedSet(Comparator.comparing(TargetVersion::release))
        var lastTargetName: JavaVersion? = null
        sorted.forEach {
            if (it.release == baseVersion.release) return@forEach // skip a runner override for base version

            val versionId = it.release.releaseId

            val ownLast = lastTargetName
            sourceSets.getByName(getTaskName(versionId, null)) versioned@{
                // depend on previous version's main source set
                val parentSet = if (ownLast != null && ownLast != baseVersion.release) { // depend on the main configuration
                    sourceSets.getByName(this@parent.getTaskName(ownLast.releaseId, null))
                } else {
                    this@parent
                }
                println("Applying configurations: $compileClasspathConfigurationName extends from ${parentSet.compileClasspathConfigurationName}")
                println("Applying configurations: $runtimeClasspathConfigurationName extends from ${parentSet.runtimeClasspathConfigurationName}")

                dependencies.add(implementationConfigurationName, parentSet.output)
                compileClasspath += parentSet.compileClasspath
                runtimeClasspath += parentSet.runtimeClasspath
                /*configurations.named(compileClasspathConfigurationName).configure {
                    extendsFrom(configurations[parentSet.compileClasspathConfigurationName])
                }
                configurations.named(runtimeClasspathConfigurationName).configure {
                    extendsFrom(configurations[parentSet.runtimeClasspathConfigurationName])
                }*/

                // Set source and target compatibility for base versions
                // Apply legacy source and target compat for Java 8 JVM
                /*tasks.named(compileJavaTaskName, JavaCompile::class) {
                    sourceCompatibility = it.release.toString()
                    targetCompatibility = it.release.toString()
                }*/
            }

            lastTargetName = it.release
        }
    }

    sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME) {
        finalizeVersions(multireleaseExtension.targetVersions, multireleaseExtension.baseVersion.get())
    }
    sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME) {
        val targets = multireleaseExtension.testTargetVersions
        val baseVersion = multireleaseExtension.baseVersion.get()
        finalizeVersions(targets, baseVersion)

        // todo: exclude base version from these
        targets.forEach { version ->
            if (version.release == baseVersion.release) return@forEach

            // Configure the testClassesDirs and runtimeClasspath
            val targetSourceSets = targets.asSequence()
                .filter { version.release > it.release }
                .map {
                    (if (it.release == baseVersion.release) this else sourceSets.getByName(getTaskName(version.release.releaseId, null)))
                        .output.classesDirs
                }
            tasks.named(this.getTaskName(version.release.releaseId, null), Test::class) {
                testClassesDirs = testClassesDirs.plus(files(*targetSourceSets.toList().toTypedArray()))
                classpath = sourceSets.getByName(getTaskName(version.release.releaseId, null)).runtimeClasspath
            }
        }
        // TODO: if not strict versions, Make base test task include every multi-release sourceSet that's <= JavaVersion.current
    }
}

// for each source set, starting from lowest to highest
// - configure target version
// - make compile + runtime classpath configurations inherit from the parent task

// -- old version -- //

/*if (baseToolchain != null && baseToolchain !in testVersions) {
    testTask.configure {
        dependsOn(mainJar)
        // Needs a jar for multi-release
        classpath = files(mainJar.get().archiveFile, classpath) - sourceSets.getByName("main").output
        // TODO: Run with targets
    }
} else {
    testVersions += JavaVersion.current()
}*/
/*
if (JavaVersion.current() !in testVersions) {
    val task = tasks.register("testJava${JavaVersion.current().majorVersion}", Test::class.java) {
        dependsOn(mainJar)
        classpath = files(mainJar.get().archiveFile, classpath) - sourceSets.getByName("main").output
    }
    checkTask.configure {
        dependsOn(task)
    }
}
 */
