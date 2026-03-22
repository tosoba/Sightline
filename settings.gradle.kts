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

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven { url = uri("https://api.mapbox.com/downloads/v2/releases/maven") }
  }
}

rootProject.name = "Sightline"

include(":app")

include(":api:overpass")
include(":api:nominatim")
include(":api:photon")

include(":core:ar")

include(":core:common")

include(":core:model")

include(":core:data")

include(":core:domain")

include(":feature:camera")

include(":feature:map")

include(":feature:places")

include(":feature:place-category")
