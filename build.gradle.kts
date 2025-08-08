plugins {
    java
    kotlin("jvm") version "2.0.21"
    id("net.minecraftforge.gradle") version "[6.0,6.2)"
}

group = "com.heledron"
version = "3.0-SNAPSHOT"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

kotlin {
    jvmToolchain(17)
}

minecraft {
    mappings("official", "1.20.1")

    runs {
        create("client") {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            mods {
                create("spideranimation") {
                    source(sourceSets.main.get())
                }
            }
        }
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
}

sourceSets.main.get().resources.srcDir("src/generated/resources")

