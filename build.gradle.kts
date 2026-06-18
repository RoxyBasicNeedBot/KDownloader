plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.skie) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.jlleitschuh.gradle.ktlint)
}

val detektVersion = libs.versions.detekt.get()

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    detekt {
        toolVersion = detektVersion
        buildUponDefaultConfig = true
        allRules = false
    }
}
