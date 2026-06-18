plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":kdownloader-core"))
    implementation(libs.sqlite.jdbc)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
}
