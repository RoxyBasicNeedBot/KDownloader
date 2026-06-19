plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.skie)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    jvm()

    iosArm64 {
        binaries.framework {
            baseName = "kdownloader_core"
        }
    }
    iosSimulatorArm64 {
        binaries.framework {
            baseName = "kdownloader_core"
        }
    }
    iosX64 {
        binaries.framework {
            baseName = "kdownloader_core"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.okio)
                implementation(libs.ktor.client.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.ktor.client.okhttp)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }

        val iosMain by getting {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}

android {
    namespace = "com.roxybasicneedbot.kdownloader.core"
    compileSdk = libs.versions.compilesdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minsdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.register("assembleKdownloader_coreXCFramework") {
    dependsOn("linkReleaseFrameworkIosArm64", "linkReleaseFrameworkIosSimulatorArm64", "linkReleaseFrameworkIosX64")
    
    doLast {
        val buildDir = layout.buildDirectory.get().asFile
        val outputDir = project.rootDir.resolve("kdownloader-flutter/ios/kdownloader_core.xcframework")
        
        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
        
        val armFramework = File(buildDir, "bin/iosArm64/releaseFramework/kdownloader_core.framework")
        val simArm64Framework = File(buildDir, "bin/iosSimulatorArm64/releaseFramework/kdownloader_core.framework")
        val simX64Framework = File(buildDir, "bin/iosX64/releaseFramework/kdownloader_core.framework")
        
        // Merge simulator frameworks (arm64 & x64) using lipo
        val mergedSimFrameworkDir = File(buildDir, "bin/iosSimulatorMerged/releaseFramework/kdownloader_core.framework")
        if (mergedSimFrameworkDir.exists()) {
            mergedSimFrameworkDir.deleteRecursively()
        }
        simArm64Framework.copyRecursively(mergedSimFrameworkDir, overwrite = true)
        
        val lipoProcess = ProcessBuilder(
            "lipo", "-create",
            File(simArm64Framework, "kdownloader_core").absolutePath,
            File(simX64Framework, "kdownloader_core").absolutePath,
            "-output", File(mergedSimFrameworkDir, "kdownloader_core").absolutePath
        ).inheritIO().start()
        if (lipoProcess.waitFor() != 0) throw GradleException("lipo failed")
        
        // Create XCFramework
        val xcodebuildProcess = ProcessBuilder(
            "xcodebuild", "-create-xcframework",
            "-framework", armFramework.absolutePath,
            "-framework", mergedSimFrameworkDir.absolutePath,
            "-output", outputDir.absolutePath
        ).inheritIO().start()
        if (xcodebuildProcess.waitFor() != 0) throw GradleException("xcodebuild failed")
    }
}


