plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.swipehome"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.cio.EngineMain"
}

kotlin {
    jvmToolchain(21)
}
dependencies {
    implementation(ktorLibs.server.cio)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.core)
    implementation(libs.logback.classic)

    implementation(libs.bundles.exposed)

    implementation(libs.postgresql)

    implementation(libs.ktor.serialization.json)

    implementation(libs.server.websockets) // Для веб-сокету

    implementation(libs.server.libs.javax.mail) // Утиліта для відправки листів

    implementation(libs.server.dotenv.kotlin) // Бібліотека для читання файлів .env

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)

}
