plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.metro)
}

android {
  namespace = "com.trm.sightline.api.photon"
  compileSdk { version = release(37) {} }

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

  implementation(libs.kotlinx.coroutines.core)

  implementation(libs.gson)
  api(libs.mapbox.geojson)

  api(libs.okhttp)
  api(libs.okhttp.logging.interceptor)
  api(libs.retrofit)
  implementation(libs.retrofit.converter.gson)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.mockwebserver)
}
