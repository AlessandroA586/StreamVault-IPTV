pluginManagement {
    repositories {
        google()
        maven("https://cache-redirector.jetbrains.com/maven-central")
        maven("https://repo1.maven.org/maven2")
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://cache-redirector.jetbrains.com/maven-central")
        maven("https://repo1.maven.org/maven2")
        mavenCentral()
    }
}

rootProject.name = "StreamVault"

include(":app")
include(":benchmark")
include(":core:navigation")
include(":core:ui")
include(":domain")
include(":data")
include(":player")
include(":feature:playback")
include(":feature:provider")
include(":feature:settings")
include(":feature:live")
include(":feature:catalog")
include(":feature:system")
include(":feature:stremio")
