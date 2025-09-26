import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.22" // Версия Kotlin
    id("org.jetbrains.compose")
    // Добавляем плагин для сериализации
    kotlin("plugin.serialization") version "1.9.22"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)

    // Зависимость для kotlinx-serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Зависимости для Exposed
    val exposedVersion = "0.49.0"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    // Драйвер для SQLite
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "kotline-calculation-electrical-systems"
            packageVersion = "1.0.0"
        }
    }
}
