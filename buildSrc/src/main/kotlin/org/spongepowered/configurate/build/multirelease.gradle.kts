import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.spongepowered.configurate.build.BASE_TARGET
import org.spongepowered.configurate.build.GRADLE_PROPERTY_PREFIX
import org.spongepowered.configurate.build.MULTIRELEASE_TARGETS
import org.spongepowered.configurate.build.OPTION_PREFIX
import java.io.File
import java.util.SortedMap
import java.util.TreeMap

internal val BASE_TARGET = JavaVersion.VERSION_1_8
private const val ENVIRONMENT_VAR_PREFIX = "JAVA_HOME_"
private const val GRADLE_PROPERTY_PREFIX = "javaHome."
private val MULTIRELEASE_TARGETS = listOf(JavaVersion.VERSION_1_9, JavaVersion.VERSION_1_10)

private fun availableJavaToolchains(project: Project): SortedMap<JavaVersion, File> {
    // GitHub actions sets variables in the format JAVA_HOME_<version>_<arch>
    // strip the <arch>, and extract the version
    val environmentProperties = System.getenv()
            .asSequence()
            .filter { (k, _) -> k.startsWith(ENVIRONMENT_VAR_PREFIX) }
            .map { (optName, path) -> JavaVersion.toVersion(optName.substring(ENVIRONMENT_VAR_PREFIX.length).substringBefore('_')) to project.file(path!!) }

    // javaHome.<version>
    val gradleProperties = project.properties
            .asSequence()
            .filter { (k, _) -> k.startsWith(GRADLE_PROPERTY_PREFIX) }
            .map { (optName, path) -> JavaVersion.toVersion(optName.substring(GRADLE_PROPERTY_PREFIX.length)) to project.file(path!!) }

    return (environmentProperties + gradleProperties).toMap(TreeMap())
}

// Set up a compile task to build for a specific Java release, including cross-compiling if possible
val availableToolchains = availableJavaToolchains(target)
fun JavaCompile.configureForRelease(version: JavaVersion) {
    sourceCompatibility = version.toString()
    targetCompatibility = version.toString()

    val toolchainVersion = availableToolchains.tailMap(version).run { if (isEmpty()) { null } else { firstKey() } } ?: JavaVersion.current()
    if (toolchainVersion < JavaVersion.current()) {
        options.isFork = true
        options.forkOptions.javaHome = availableToolchains[toolchainVersion]
        if (!toolchainVersion.isJava9Compatible) {
            options.errorprone.isEnabled.set(false)
        }
    }
    if (toolchainVersion.isJava9Compatible) {
        options.compilerArgs.addAll(listOf("--release", version.majorVersion))
    }
}

val sourceSets = extensions.getByType(SourceSetContainer::class.java)
val existingSets = sourceSets.toSet()
val testTask = tasks.named(JavaPlugin.TEST_TASK_NAME, Test::class.java)
val checkTask = tasks.named("check")
val testVersions = mutableSetOf<JavaVersion>()

// Multi release jars
existingSets.forEach { set ->
    val jarTask = tasks.findByName(set.jarTaskName) as? Jar ?: return@forEach
    jarTask.manifest.attributes["Multi-Release"] = "true"
    // Based on guidance at https://blog.gradle.org/mrjars
    // and an example at https://github.com/melix/mrjar-gradle

    MULTIRELEASE_TARGETS.forEach { targetVersion ->
        if (targetVersion <= org.spongepowered.configurate.build.BASE_TARGET) {
            throw GradleException("Cannot build version $targetVersion as it is lower than (or equal to?) the project's base version (${org.spongepowered.configurate.build.BASE_TARGET})")
        } else if (targetVersion > JavaVersion.current()) {
            throw GradleException("Java version $targetVersion is required to build this project, and you are running ${JavaVersion.current()}!")
        }

        val versionId = "java${targetVersion.majorVersion}"
        sourceSets.register(set.getTaskName(versionId, null)) { version ->
            version.java.srcDirs(project.projectDir.resolve("src/${set.name}/$versionId"))
            // Depend on main source set
            project.dependencies.add(version.implementationConfigurationName, set.output.classesDirs)?.apply {
                // this.builtBy(tasks.named(set.compileJavaTaskName))
            }
            // Set compatibility
            tasks.named(version.compileJavaTaskName, JavaCompile::class.java) {
                it.configureForRelease(targetVersion)
            }
            // Add to output jar
            jarTask.into("META-INF/versions/${targetVersion.majorVersion}") {
                it.from(version.output)
            }

            val toolchainVersion = availableToolchains.tailMap(targetVersion).run { if (isEmpty()) { null } else { firstKey() } }
            if (toolchainVersion != null && toolchainVersion < JavaVersion.current()) {
                testVersions += toolchainVersion
                val versionedTest = tasks.register(set.getTaskName("test", versionId), Test::class.java) {
                    it.dependsOn(jarTask)
                    it.classpath = files(jarTask.archiveFile, it.classpath) - set.output
                    it.executable = availableToolchains[toolchainVersion]?.resolve("bin/java").toString()
                }
                checkTask.configure {
                    it.dependsOn(versionedTest)
                }
            }
        }
    }
}

val mainJar = tasks.named(JavaPlugin.JAR_TASK_NAME, Jar::class.java)
val baseToolchain = availableToolchains.tailMap(BASE_TARGET).run { if (isEmpty()) { null } else { firstKey() } }
if (baseToolchain != null && baseToolchain !in testVersions) {
    testVersions += baseToolchain
    testTask.configure {
        it.dependsOn(mainJar)
        it.classpath = files(mainJar.get().archiveFile, it.classpath) - sourceSets.getByName("main").output
        it.executable = availableToolchains[baseToolchain]?.resolve("bin/java").toString()
    }
} else {
    testVersions += JavaVersion.current()
}

if (JavaVersion.current() !in testVersions) {
    val task = tasks.register("testJava${JavaVersion.current().majorVersion}", Test::class.java) {
        it.dependsOn(mainJar)
        it.classpath = files(mainJar.get().archiveFile, it.classpath) - sourceSets.getByName("main").output
    }
    checkTask.configure {
        it.dependsOn(task)
    }
}
