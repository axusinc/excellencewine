plugins {
    kotlin("jvm") version "2.1.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10"
    application
}

group = "eth.likespro"
version = "0.0.1"

application {
    mainClass = "app.MainKt"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("io.github.likespro:commons-core:3.1.0")
    implementation("io.github.likespro:commons-reflection:3.1.0")

    implementation("io.github.likespro:atomarix-core:1.0.0-disabled")
    implementation("io.github.likespro:atomarix-exposed:1.0.0-disabled")

//    implementation("org.telegram:telegrambots-longpolling:8.3.0")
//    implementation("org.telegram:telegrambots-longpolling:8.3.0")
    implementation("dev.inmo:tgbotapi:25.0.1")
    implementation("io.ktor:ktor-serialization-gson:3.1.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("io.insert-koin:koin-ktor:3.5.0")
    implementation("org.jetbrains.exposed:exposed-core:0.61.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.61.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.61.0")
    implementation ("org.postgresql:postgresql:42.7.2")

    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("ch.qos.logback:logback-classic:1.5.13")
}
