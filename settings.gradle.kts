pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "mpcharts-cmp"

include(":mpchart")
include(":sample:shared")
include(":sample:android")
include(":sample:desktop")
