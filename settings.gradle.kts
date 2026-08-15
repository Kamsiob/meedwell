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

rootProject.name = "Meedwell"

// Two modules, decided before the build started. `:core` is pure Kotlin with
// no Android plugin applied, which is what makes the no-Android-dependency
// rule fail the build rather than go unnoticed. See ARCHITECTURE.md.
include(":core")
include(":app")
