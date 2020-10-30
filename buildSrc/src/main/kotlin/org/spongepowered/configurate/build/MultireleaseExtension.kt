package org.spongepowered.configurate.build

import org.gradle.api.Action
import org.gradle.api.DomainObjectSet
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.domainObjectSet
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

const val MULTIRELEASE_EXTENSION = "multirelease"

/**
 * A specification for the exact target version to be used for both source and execution
 */
data class TargetVersion(val release: JavaVersion, val jvm: JavaVersion, val extraConfigureAction: Action<JavaCompile> = Action {}) {
    constructor(version: JavaVersion) : this(version, version)

    init {
        require(jvm >= release) { "The chosen JVM ($jvm) must have a version of $release or newer to compile source for Java $release" }
    }
}

/**
 * Configure multi-release jar creation
 */
open class MultireleaseExtension @Inject constructor(objects: ObjectFactory, providers: ProviderFactory) {
    /**
     * Whether to strictly apply toolchain versions.
     *
     * If this is set to false, java toolchains will only be overridden for
     * the Gradle JVM's version is less than the target version.
     */
    val strictVersions: Property<Boolean> = objects.property<Boolean>()
        .convention(
            providers.gradleProperty("strictMultireleaseVersions")
                .orElse(providers.environmentVariable("CI")) // set by GH Actions and Travis
                .map(String::toBoolean)
                .orElse(false)
        )

    /**
     * The minimum source and compiler versions, for the `main` source set.
     *
     * No compile tasks will run with JDK versions less than the one specified here
     */
    internal val baseVersion: Property<TargetVersion> = objects.property()

    /**
     * Target versions for the main source set.
     *
     * If there is more than one version declared, the `jar` produced by the
     * main source set will have the `Multi-Release` property set to `true`.
     *
     * There can be no more than one version that is less than Java 9
     * (the version of Java that introduced multi-release jars) in this set.
     */
    internal val targetVersions: DomainObjectSet<TargetVersion> = objects.domainObjectSet(TargetVersion::class)

    /**
     * Target versions for compiling and running tests.
     *
     * By default, this will use the same versions as declared for [targetVersions].
     */
    val testTargetVersions: DomainObjectSet<TargetVersion> = objects.domainObjectSet(TargetVersion::class)

    /**
     * The base version for the source set without any additional names.
     *
     * All multirelease variants will be built with at least the JDK version specified here
     */
    @JvmOverloads
    fun baseVersion(source: JavaVersion, compileWith: JavaVersion = source) {
        targetVersions.forEach {
            if (it.release <= source) {
                throw GradleException("Cannot set project base Java version to a version newer than multirelease variant $it")
            }
        }
        this.baseVersion.set(TargetVersion(source, compileWith))
    }

    @JvmOverloads
    fun targetVersion(source: JavaVersion, compileWith: JavaVersion = source, type: TargetType = TargetType.BOTH) {
        if (type.main) this.targetVersions.add(TargetVersion(source, compileWith))
        if (type.test) this.testTargetVersions.add(TargetVersion(source, compileWith))
    }
}

/**
 * Whether a target version will be applied to the main source set only, the test source set only, or both
 */
enum class TargetType(internal val main: Boolean, internal val test: Boolean) {
    MAIN(true, false),
    TEST(false, true),
    BOTH(true, true)
}
