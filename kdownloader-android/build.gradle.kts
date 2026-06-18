plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.roxybasicneedbot.kdownloader.android"
    compileSdk = libs.versions.compilesdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minsdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    api(project(":kdownloader-core"))

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(kotlin("test"))
    androidTestImplementation(libs.androidx.work.testing)
}
