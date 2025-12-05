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

    // Зависимости для работы с БД (Exposed + SQLite)
    implementation("org.xerial:sqlite-jdbc:3.41.2.2")

    // НОВОЕ: Зависимость для логирования. Убирает предупреждение SLF4J.
    implementation("org.slf4j:slf4j-simple:2.0.7")
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe)
            packageName = "kotline-calculation-electrical-systems"
            packageVersion = "1.0.0"
            modules("java.instrument", "java.prefs", "java.sql", "jdk.unsupported")

            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }
        }
    }
}
