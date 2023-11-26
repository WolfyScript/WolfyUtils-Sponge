import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.wolfyscript.devtools.docker.minecraft.MinecraftServersExtension
import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency
import java.io.FileOutputStream

plugins {
    `java-library`
    id("org.spongepowered.gradle.plugin") version "2.1.1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.wolfyscript.devtools.docker.run") version ("2.0-SNAPSHOT")
    id("com.wolfyscript.devtools.docker.minecraft_servers") version ("2.0-SNAPSHOT")
}

val serverDir: String by project
group = "com.wolfyscript.wolfyutils.sponge"
version = "5.0-alpha.1-SNAPSHOT"

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
    apiVersion("8.2.0")
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

val javaTarget = 17 // Sponge targets a minimum of Java 8
java {
    sourceCompatibility = JavaVersion.toVersion(javaTarget)
    targetCompatibility = JavaVersion.toVersion(javaTarget)
    if (JavaVersion.current() < JavaVersion.toVersion(javaTarget)) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(javaTarget))
    }
}

tasks.withType(ShadowJar::class) {
    archiveClassifier.set("")
    //archiveVersion.set("")
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

val debugPort: String = "5007"

minecraftDockerRun {
    val customEnv = env.get().toMutableMap()
    customEnv["MEMORY"] = "2G"
    customEnv["JVM_OPTS"] = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:${debugPort}"
    env.set(customEnv)
    arguments("--cpus", "2", "-it") // Constrain to only use 2 cpus, and allow for console interactivity with 'docker attach'
}

minecraftServers {
    val directory = file("${System.getProperty("user.home")}${File.separator}minecraft${File.separator}test_servers_v5");
    serversDir.set(directory)
    libName.set("${project.name}-${version}.jar")
    val debugPortMapping = "${debugPort}:${debugPort}"
    servers {
        register("spongevanilla_1_16") {
            val spongeVersion = "1.16.5-8.2.0"
            type.set("CUSTOM")
            extraEnv.put("SPONGEVERSION", spongeVersion)
            extraEnv.put("CUSTOM_SERVER", "https://repo.spongepowered.org/repository/maven-public/org/spongepowered/spongevanilla/${spongeVersion}/spongevanilla-${spongeVersion}-universal.jar")
            ports.set(setOf(debugPortMapping, "25595:25565"))
        }
    }
}
