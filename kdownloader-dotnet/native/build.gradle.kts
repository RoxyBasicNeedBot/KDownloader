plugins {
    kotlin("multiplatform")
}

kotlin {
    // Windows DLL
    mingwX64("nativeWin") {
        binaries {
            sharedLib {
                baseName = "kdownloader"
            }
        }
    }
    // Linux SO
    linuxX64("nativeLinux") {
        binaries {
            sharedLib {
                baseName = "kdownloader"
            }
        }
    }
    // macOS dylib
    macosX64("nativeMacX64") {
        binaries {
            sharedLib {
                baseName = "kdownloader"
            }
        }
    }
    macosArm64("nativeMacArm") {
        binaries {
            sharedLib {
                baseName = "kdownloader"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":kdownloader-core"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}
