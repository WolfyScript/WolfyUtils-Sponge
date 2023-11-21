import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
    `java-library`
    id("org.spongepowered.gradle.plugin") version "2.1.1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

val serverDir: String by project
group = "com.wolfyscript.wolfyutils.sponge"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral();
    mavenLocal();
    maven("https://maven.wolfyscript.com/repository/public/")
}

dependencies {
    implementation("com.wolfyscript.wolfyutils", "wolfyutilities","5.0-SNAPSHOT")
    implementation("com.fasterxml.jackson.core", "jackson-databind","2.15.3")
    implementation("org.reflections", "reflections", "0.10.2")
    implementation("it.unimi.dsi:fastutil:8.5.6")
    implementation("org.jetbrains:annotations:24.0.0")
}

sponge {
    apiVersion("8.1.0")
    license("GNU GPL 3.0")
    loader {
        name(PluginLoaders.JAVA_PLAIN)
        version("1.0")
    }
    plugin("wolfyutils") {
        displayName("WolfyUtilities")
        entrypoint("com.wolfyscript.utilities.sponge.WolfyCoreSponge")
        description("My plugin description")
        links {
            // homepage("https://spongepowered.org")
            // source("https://spongepowered.org/source")
            // issues("https://spongepowered.org/issues")
        }
        dependency("spongeapi") {
            loadOrder(PluginDependency.LoadOrder.AFTER)
            optional(false)
        }
    }
}

val javaTarget = 16 // Sponge targets a minimum of Java 8
java {
    sourceCompatibility = JavaVersion.toVersion(javaTarget)
    targetCompatibility = JavaVersion.toVersion(javaTarget)
    if (JavaVersion.current() < JavaVersion.toVersion(javaTarget)) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(javaTarget))
    }
}

tasks.withType(ShadowJar::class) {
    archiveBaseName.set("wolfyutils-sponge")
    archiveClassifier.set("")
    archiveVersion.set("")
    relocate("com.fasterxml", "com.wolfyscript.lib.com.fasterxml")
    relocate("org.reflections", "com.wolfyscript.lib.org.reflections")
    relocate("javassist", "com.wolfyscript.lib.javassist")
    relocate("javax.annotation", "com.wolfyscript.lib.javax.annotation")
    relocate("org.slf4j", "com.wolfyscript.lib.org.slf4j")
    relocate("com.typesafe", "com.wolfyscript.lib.com.typesafe")
    relocate("it.unimi.dsi", "com.wolfyscript.lib.it.unimi.dsi")
}

tasks.withType(JavaCompile::class).configureEach {
    options.apply {
        encoding = "utf-8" // Consistent source file encoding
        if (JavaVersion.current().isJava10Compatible) {
            release.set(javaTarget)
        }
    }
}

// Make sure all tasks which produce archives (jar, sources jar, javadoc jar, etc) produce more consistent output
tasks.withType(AbstractArchiveTask::class).configureEach {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
}

tasks.register<Copy>("copyJar") {
    println("Copy Jar to server: $serverDir/plugins")
    from(layout.buildDirectory.dir("libs/wolfyutils-sponge.jar"))
    into("$serverDir/plugins")
}
