plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":kdownloader-core"))
    implementation(project(":kdownloader-desktop"))
    implementation(libs.kotlinx.coroutines.core)
}

application {
    mainClass.set("com.roxybasicneedbot.kdownloader.sample.MainKt")
}
