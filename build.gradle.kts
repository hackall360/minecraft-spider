plugins {
    java
    kotlin("jvm") version "1.9.24"
    id("net.minecraftforge.gradle") version "[6.0,6.2)"
}

val modVersion: String by project

group = "com.heledron"
version = modVersion

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

kotlin {
    jvmToolchain(17)
}

minecraft {
    mappings("official", "1.20.1")

    runs {
        create("server") {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            mods {
                create("spideranimation") {
                    source(sourceSets.main.get())
                }
            }
        }
        create("data") {
            workingDirectory(project.file("run"))
            args("--mod", "spideranimation", "--all", "--output", file("src/generated/resources/"), "--existing", file("src/main/resources/"))
            mods {
                create("spideranimation") {
                    source(sourceSets.main.get())
                }
            }
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.minecraftforge.net")
}

dependencies {
    minecraft("net.minecraftforge:forge:1.20.1-47.3.0")
    implementation(kotlin("stdlib"))
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation(kotlin("test-junit5"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    inputs.property("version", modVersion)
    filesMatching("META-INF/mods.toml") {
        expand("version" to modVersion)
    }
}

sourceSets.main.get().resources.srcDir("src/generated/resources")

