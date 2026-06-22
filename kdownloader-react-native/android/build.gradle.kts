plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.roxybasicneedbot.kdownloader.reactnative"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":kdownloader-core"))
    implementation(project(":kdownloader-android"))
    
    // React Native dependencies are provided by the consuming app
    compileOnly("com.facebook.react:react-android:+")
    
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}
