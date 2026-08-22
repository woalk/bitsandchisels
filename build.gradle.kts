plugins {
    id("net.fabricmc.fabric-loom") version "1.17.18"
    val kotlinVersion = "2.4.10"
    kotlin("jvm").version(kotlinVersion)
}
base {
    val archivesBaseName = providers.gradleProperty("archivesBaseName").get()
    archivesName.set(archivesBaseName)
}
val modVersion = providers.gradleProperty("modVersion").get()
version = modVersion
val mavenGroup = providers.gradleProperty("mavenGroup").get()
group = mavenGroup
repositories {
    maven("https://jitpack.io")
    maven("https://maven.enginehub.org/repo/")
}
dependencies {
    val minecraftVersion = providers.gradleProperty("minecraftVersion").get()
    minecraft("com.mojang:minecraft:$minecraftVersion")
    val loaderVersion = providers.gradleProperty("loaderVersion").get()
    implementation("net.fabricmc:fabric-loader:$loaderVersion")
    val fabricVersion = providers.gradleProperty("fabricVersion").get()
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")
    val fabricKotlinVersion = providers.gradleProperty("fabricKotlinVersion").get()
    implementation("net.fabricmc:fabric-language-kotlin:$fabricKotlinVersion")

    val fabricPermissionsApiVersion = providers.gradleProperty("fabricPermissionsApiVersion").get()
    include(implementation("me.lucko:fabric-permissions-api:${fabricPermissionsApiVersion}")!!)

    val worldEditVersion = providers.gradleProperty("worldEditVersion").get()
    implementation("com.sk89q.worldedit:worldedit-fabric-mc${minecraftVersion}:$worldEditVersion")
}
tasks {
    val javaVersion = JavaVersion.VERSION_25
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(javaVersion.toString().toInt())
    }
    jar { from("LICENSE") { rename { "${it}_${base.archivesName}" } } }
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") { expand(mutableMapOf("version" to project.version)) }
    }
    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion.toString())) }
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        withSourcesJar()
    }
}
loom {
    accessWidenerPath.set(file("src/main/resources/bitsandchisels.accesswidener"))
}