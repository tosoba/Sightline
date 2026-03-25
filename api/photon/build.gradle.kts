plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.hilt)
  alias(libs.plugins.ksp)
}

android {
  namespace = "com.trm.sightline.api.photon"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

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

  implementation(libs.kotlinx.coroutines.core)

  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  implementation(libs.gson)
  api(libs.mapbox.geojson)

  implementation(libs.okhttp)
  implementation(libs.retrofit)
  implementation(libs.retrofit.converter.gson)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
