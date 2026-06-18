plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":kdownloader-core"))
    implementation(libs.sqlite.add.something.here.if.needed) // wait, use the libs catalog entry
    implementation(libs.sqlite.jdbc)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
}
