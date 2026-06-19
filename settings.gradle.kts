pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "KDownloader"

include(":kdownloader-core")
include(":kdownloader-android")
// include(":kdownloader-desktop")
include(":kdownloader-hilt")
include(":kdownloader-compose")
// include(":kdownloader-dotnet-native")

// project(":kdownloader-dotnet-native").projectDir = file("kdownloader-dotnet/native")
