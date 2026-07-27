import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension
import net.neoforged.moddevgradle.legacyforge.dsl.MixinExtension
import net.neoforged.moddevgradle.legacyforge.dsl.ObfuscationExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    idea
    `java-library`
    `maven-publish`
    id("org.jetbrains.kotlin.jvm") version "2.3.20"
    id("net.neoforged.moddev.legacyforge") version "2.0.91"
}

val modId = providers.gradleProperty("mod_id").get()
val minecraftVersionValue = providers.gradleProperty("minecraft_version").get()
val forgeVersionValue = providers.gradleProperty("forge_version").get()
version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("mod_group_id").get()

base {
    archivesName.set(modId)
}

repositories {
    // Long-lived development mods are resolved from lib before any remote repository.
    flatDir {
        dirs("lib")
    }
    flatDir {
        dirs("mods")
    }
    mavenLocal()
    mavenCentral()
    maven {
        name = "JaredsMaven"
        url = uri("https://maven.blamejared.com/")
    }
    maven {
        name = "ModMaven"
        url = uri("https://modmaven.dev")
    }
    maven {
        name = "KotlinForForge"
        url = uri("https://thedarkcolour.github.io/KotlinForForge/")
    }
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter { includeGroup("maven.modrinth") }
    }
    exclusiveContent {
        forRepository {
            maven { url = uri("https://maven.tterrag.com/") }
        }
        filter { includeGroup("com.tterrag.registrate") }
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

kotlin {
    jvmToolchain(17)
}

// Long-lived development dependencies. ModDevGradle remaps these before the
// Forge run, while the regular jar tasks never package them.
val libModFiles = listOf(
    file("lib/GlitchCore-forge-1.20.1-0.0.1.1.jar"),
    file("lib/SereneSeasons-forge-1.20.1-9.1.0.3.jar"),
    file("lib/sereneseasonsfix-1.20.2-1.1.1.0.jar"),
    file("lib/Jade-1.20.1-Forge-11.13.3.jar"),
    file("lib/jei-1.20.1-forge-15.20.0.129.jar"),
    file("lib/farmersdelight-1.20.1-1.2.9.jar"),
    file("lib/list-3.0.7.jar") ,
    file("lib/kubejs-forge-2001.6.5-build.26.jar"),
    file("lib/CraftTweaker-forge-1.20.1-14.0.60.jar")

).filter(File::exists)
// Temporary test mods. Their coordinates are derived from each file name and
// resolved from this local directory, so adding a JAR requires no Gradle edits.
val testModFiles = fileTree(layout.projectDirectory.dir("mods")) {
    include("*.jar")
}

val testModInput = configurations.maybeCreate("testModInput").apply {
    isCanBeConsumed = false
}
val testModRuntime = extensions
    .getByType<ObfuscationExtension>()
    .createRemappingConfiguration(testModInput)

val libRuntime = configurations.maybeCreate("libRuntime")
configurations.named("runtimeClasspath") {
    extendsFrom(libRuntime)
    extendsFrom(testModInput)
}

val modLibRuntime = extensions
    .getByType<ObfuscationExtension>()
    .createRemappingConfiguration(libRuntime)

fun localModCoordinate(file: File): String {
    val stem = file.nameWithoutExtension
    val separator = stem.lastIndexOf('-')
    require(separator > 0) { "Local mod JAR must contain a name-version separator: ${file.name}" }
    return "local:${stem.substring(0, separator)}:${stem.substring(separator + 1)}"
}

val libModCoordinates = libModFiles.map(::localModCoordinate)

extensions.configure<LegacyForgeExtension> {
    version = "$minecraftVersionValue-$forgeVersionValue"

    parchment {
        mappingsVersion = providers.gradleProperty("parchment_mappings_version").get()
        minecraftVersion = providers.gradleProperty("parchment_minecraft_version").get()
    }

    runs {
        register("client") {
            client()
            systemProperty("mixin.env.remapRefMap", "true")
            systemProperty("forge.enabledGameTestNamespaces", modId)
        }
        register("server") {
            server()
            programArgument("--nogui")
            systemProperty("forge.enabledGameTestNamespaces", modId)
        }
        register("gameTestServer") {
            type = "gameTestServer"
            systemProperty("forge.enabledGameTestNamespaces", modId)
        }
        register("data") {
            data()
            programArguments.addAll(
                "--mod", modId,
                "--all",
                "--output", file("src/generated/resources/").absolutePath,
                "--existing", file("src/main/resources/").absolutePath
            )
        }

        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

extensions.configure<MixinExtension> {
    config("farmerstales.mixin.json")
    add(sourceSets.main.get(), "farmerstales.refmap.json")
}

sourceSets.named("main") {
    resources.srcDir("src/generated/resources")
}

dependencies {
    jarJar(modApi(libs.registrate.get())!!)
    modImplementation("thedarkcolour:kotlinforforge:4.12.0")
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")

    libModCoordinates.forEach { add(modLibRuntime.name, it) }
    testModFiles.files
        .map(::localModCoordinate)
        .forEach { add(testModRuntime.name, it) }

    // Optional API jars are compile-time only; the actual mods stay development-only.
    add("compileOnly", files("lib/Jade-1.20.1-Forge-11.13.3.jar"))
    add("compileOnly", files("lib/jei-1.20.1-forge-15.20.0.129.jar"))
    add("compileOnly", files("lib/SereneSeasons-forge-1.20.1-9.1.0.3.jar"))
    add("compileOnly", files("lib/kubejs-forge-2001.6.5-build.26.jar"))
    add("compileOnly", files("lib/CraftTweaker-forge-1.20.1-14.0.60.jar"))

}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = mapOf(
        "minecraft_version" to providers.gradleProperty("minecraft_version").get(),
        "minecraft_version_range" to providers.gradleProperty("minecraft_version_range").get(),
        "forge_version" to providers.gradleProperty("forge_version").get(),
        "forge_version_range" to providers.gradleProperty("forge_version_range").get(),
        "loader_version_range" to providers.gradleProperty("loader_version_range").get(),
        "mod_id" to modId,
        "mod_name" to providers.gradleProperty("mod_name").get(),
        "mod_license" to providers.gradleProperty("mod_license").get(),
        "mod_version" to providers.gradleProperty("mod_version").get(),
        "mod_authors" to providers.gradleProperty("mod_authors").get(),
        "mod_description" to providers.gradleProperty("mod_description").get()
    )

    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from("src/main/templates")
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}

sourceSets.named("main") {
    resources.srcDir(generateModMetadata)
}

extensions.configure<LegacyForgeExtension> {
    ideSyncTask(generateModMetadata)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven { url = uri(layout.projectDirectory.dir("repo")) }
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
