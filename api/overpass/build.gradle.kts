plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.hilt)
  alias(libs.plugins.ksp)
}

import java.util.Properties

val localProperties =
  Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
    if (getProperty("overpass.email") == null) {
      logger.warn(
        "overpass.email is not set in local.properties - add it to identify the app to the Overpass API server (see README Setup)"
      )
    }
  }

android {
  namespace = "com.trm.sightline.api.overpass"
  compileSdk { version = release(37) {} }

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    buildConfigField(
      type = "String",
      name = "OVERPASS_EMAIL",
      value = "\"${localProperties.getProperty("overpass.email", "")}\"",
    )
  }

  buildFeatures { buildConfig = true }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

dependencies {
  implementation(project(":core:common"))

  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  implementation(libs.kotlinx.coroutines.core)

  implementation(libs.moshi)
  implementation(libs.moshi.adapters)
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging.interceptor)
  implementation(libs.retrofit)
  implementation(libs.retrofit.converter.moshi)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
