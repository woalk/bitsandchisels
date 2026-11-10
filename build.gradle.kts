import java.net.URI

plugins {
    id("net.fabricmc.fabric-loom") version "1.17.21"
    val kotlinVersion = "2.4.21"
    kotlin("jvm").version(kotlinVersion)
    id("com.modrinth.minotaur") version "2.+"
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
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = URI("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
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

    // Woalk Datafixer
    compileOnly("maven.modrinth:eolKiaOU:HvHFQ4Rq")
}
tasks {
    val javaVersion = JavaVersion.VERSION_25
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(javaVersion.toString().toInt())
    }
    jar { from("LICENSE") { rename { "${it}_${base.archivesName}" } } }
    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion.toString())) }
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        withSourcesJar()
    }
}

tasks.processResources {
    inputs.property("modVersion", project.version)
    inputs.property("minecraftVersion", project.property("minecraftVersion")!!)
    inputs.property("loaderVersion", project.property("loaderVersion")!!)
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraftVersion" to project.property("minecraftVersion")!!,
            "loaderVersion" to project.property("loaderVersion")!!,
            "fabricKotlinVersion" to project.property("fabricKotlinVersion")!!
        )
    }
}

loom {
    accessWidenerPath.set(file("src/main/resources/bitsandchisels.accesswidener"))
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set("bitsandchisels")
    versionType.set("beta")
    uploadFile.set(tasks.jar)
    dependencies {
        required.project("fabric-api")
        required.project("fabric-language-kotlin")
        optional.project("worldedit")
        optional.project("luckperms")
        optional.project("woalk-datafixer")
        embedded.project("fabric-permissions-api")
    }
}
