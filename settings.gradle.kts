pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { mavenCentral() }
}

rootProject.name = "ares-framework"
includeBuild("build-logic")
include(":modules:ares-annotations", ":modules:ares-processor", ":modules:ares-runtime", ":modules:ares-test")
