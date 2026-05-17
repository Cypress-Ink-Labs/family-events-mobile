pluginManagement {
    repositories {
        google()
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

rootProject.name = "FamilyEventsAndroid"

include(
    ":app",
    ":core",
    ":data",
    ":designsystem",
    ":auth",
    ":plan",
    ":explore",
    ":saved",
    ":eventdetail",
    ":platform",
)
