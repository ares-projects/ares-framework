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
include("ares-annotations", "ares-runtime", "ares-processor", "examples:hello-lambda")
