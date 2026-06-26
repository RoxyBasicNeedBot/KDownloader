plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.skie) apply false
    alias(libs.plugins.kover)
    alias(libs.plugins.detekt)
    alias(libs.plugins.jlleitschuh.gradle.ktlint)
}

val detektVersion = libs.versions.detekt.get()

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "maven-publish")

    detekt {
        toolVersion = detektVersion
        buildUponDefaultConfig = true
        allRules = false
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    plugins.withId("com.android.library") {
        configure<org.gradle.api.publish.PublishingExtension> {
            publications {
                create<org.gradle.api.publish.maven.MavenPublication>("release") {
                    afterEvaluate {
                        from(components["release"])
                    }
                }
            }
        }
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        configure<org.gradle.api.publish.PublishingExtension> {
            publications {
                create<org.gradle.api.publish.maven.MavenPublication>("mavenJava") {
                    from(components["java"])
                }
            }
        }
    }
}


