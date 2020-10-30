import org.spongepowered.configurate.build.TargetType

plugins {
    id("org.spongepowered.configurate.build.component")
}

dependencies {
    api("io.leangen.geantyref:geantyref:1.3.11")
    compileOnlyApi("org.checkerframework:checker-qual:3.7.0")
    compileOnly("com.google.auto.value:auto-value-annotations:1.7.4")
    annotationProcessor("com.google.auto.value:auto-value:1.7.4")
    testImplementation("com.google.guava:guava:30.0-jre")
}

multirelease {
    targetVersion(JavaVersion.VERSION_1_9) // module descriptor
    targetVersion(JavaVersion.VERSION_1_10) // immutable collection types
    targetVersion(JavaVersion.VERSION_15)
    targetVersion(JavaVersion.VERSION_1_8, JavaVersion.VERSION_1_8, TargetType.TEST) // run base tests on a Java 8 JVM
}

// Enable JDK 15 preview  features
tasks.named("compileJava15TestJava", JavaCompile::class).configure {
    options.compilerArgs.addAll(listOf("--enable-preview", "-Xlint:-preview")) // For records
}

tasks.named("java15Test", Test::class).configure {
    jvmArgs("--enable-preview")
}

// Set up Java 14 tests for record support

/*
val java14Test by sourceSets.registering {
    val testDir = file("src/test/java14")
    java.srcDir(testDir)

    tasks.named<JavaCompile>(compileJavaTaskName).configure {
        options.compilerArgs.addAll(listOf("--enable-preview", "-Xlint:-preview")) // For records
    }

    dependencies.add(implementationConfigurationName, sourceSets.main.map { it.output })

    configurations.named(compileClasspathConfigurationName).configure { extendsFrom(configurations.testCompileClasspath.get()) }
    configurations.named(runtimeClasspathConfigurationName).configure { extendsFrom(configurations.testRuntimeClasspath.get()) }
}

tasks.test {
    testClassesDirs += java14Test.get().output.classesDirs
    classpath += java14Test.get().runtimeClasspath
    dependsOn(tasks.named(java14Test.get().compileJavaTaskName))
    jvmArgs("--enable-preview") // For records
}
 **/
