import net.ltgt.gradle.errorprone.errorprone
import org.spongepowered.configurate.build.core
import org.spongepowered.configurate.build.useAutoValue

plugins {
    id("org.spongepowered.configurate.build.component")
}

useAutoValue()
dependencies {
    api(core())
    // When updating snakeyaml, check ConfigurateScanner for changes against upstream
    implementation("org.yaml:snakeyaml:1.27")
}

tasks.compileJava {
    options.errorprone.excludedPaths.set(".*org[\\\\/]spongepowered[\\\\/]configurate[\\\\/]yaml[\\\\/]ConfigurateScanner.*")
    // our vendored version of ScannerImpl has invalid JD, so we have to suppress some warnings
    options.compilerArgs.add("-Xdoclint:-html")
}
