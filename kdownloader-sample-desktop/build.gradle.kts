plugins {
    kotlin("jvm")
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    application
}

dependencies {
    implementation(project(":kdownloader-core"))
    implementation(project(":kdownloader-desktop"))
    implementation(libs.kotlinx.coroutines.core)
    
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.ui)
}

application {
    mainClass.set("com.roxybasicneedbot.kdownloader.sample.desktop.MainKt")
}
