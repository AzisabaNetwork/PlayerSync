plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.serialization") version "1.8.10"
    id("com.github.johnrengelman.shadow") version "8.0.0"
    `maven-publish`
}

group = "net.azisaba"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo.azisaba.net/repository/maven-public/") }
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
}

dependencies {
    // kotlin
    implementation(kotlin("stdlib"))
    // serialization
    implementation("com.charleskorn.kaml:kaml:0.52.0")
    implementation("org.yaml:snakeyaml:2.0")
    implementation("xyz.acrylicstyle.java-util:serialization:1.2.1-SNAPSHOT")
    compileOnly("io.netty:netty-buffer:4.1.79.Final")
    // vault
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    // redis
    implementation("redis.clients:jedis:4.3.1")
    // spigot & paper
    compileOnly("com.destroystokyo.paper:paper-api:1.15.2-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot:1.15.2-R0.1-SNAPSHOT")
    // annotations
    compileOnly("org.jetbrains:annotations:24.0.1")
}

tasks {
    test {
        useJUnitPlatform()
    }

    compileKotlin {
        kotlinOptions {
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
        }
    }

    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        from(sourceSets.main.get().resources.srcDirs) {
            filter(org.apache.tools.ant.filters.ReplaceTokens::class, mapOf("tokens" to mapOf("version" to project.version.toString())))
            filteringCharset = "UTF-8"
        }
    }

    shadowJar {
        exclude("org/slf4j/**")
        relocate("kotlin", "net.azisaba.playersync.lib.kotlin")
        relocate("org.jetbrains", "net.azisaba.playersync.lib.org.jetbrains")
        relocate("com.charleskorn.kaml", "net.azisaba.playersync.lib.com.charleskorn.kaml")
        relocate("org.yaml", "net.azisaba.playersync.lib.org.yaml")
        relocate("org.snakeyaml", "net.azisaba.playersync.lib.org.snakeyaml")
        relocate("org.intellij", "net.azisaba.playersync.lib.org.intellij")
        relocate("xyz.acrylicstyle", "net.azisaba.playersync.lib.xyz.acrylicstyle")
        relocate("redis.clients", "net.azisaba.playersync.lib.redis.clients")
    }
}

publishing {
    repositories {
        maven {
            name = "repo"
            credentials(PasswordCredentials::class)
            url = uri(
                if (project.version.toString().endsWith("SNAPSHOT"))
                    project.findProperty("deploySnapshotURL") ?: System.getProperty("deploySnapshotURL", "https://repo.azisaba.net/repository/maven-snapshots/")
                else
                    project.findProperty("deployReleasesURL") ?: System.getProperty("deployReleasesURL", "https://repo.azisaba.net/repository/maven-releases/")
            )
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

kotlin {
    jvmToolchain(8)
}
