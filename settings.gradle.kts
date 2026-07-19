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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FieldFlow"
include(":app")
include(":domain")
include(":data")
include(":core:navigation")
include(":core:database")
include(":core:designsystem")
include(":core:testing")
include(":feature:dashboard")
include(":feature:assets")
include(":feature:templates")
include(":feature:inspection")
include(":feature:issues")
include(":feature:reports")
