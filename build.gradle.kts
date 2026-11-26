// Top-level build file where you can add configuration options common to all sub-projects/modules.

import org.gradle.api.tasks.Copy

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.kmp.library) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.metro) apply false
  alias(libs.plugins.skie) apply false
}

val kmpHostTestProjects =
  listOf(":core:model", ":core:domain", ":core:common", ":core:data", ":core:datastore", ":api:photon", ":core:ar")

tasks.register("test") {
  description = "Runs the Android unit tests and the KMP modules' Android host tests."
  group = "verification"
  dependsOn(kmpHostTestProjects.map { "$it:testAndroidHostTest" })
}

val syncIosMapStyles = tasks.register<Copy>("syncIosMapStyles") {
  description = "Copies the MapLibre style JSONs used by Android into the iOS app resources."
  from(layout.projectDirectory.dir("core/ui/src/main/res/raw")) {
    include("light_style.json", "dark_style.json")
  }
  into(layout.projectDirectory.dir("iosApp/Resources"))
}

