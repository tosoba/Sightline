plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.hilt)
  alias(libs.plugins.ksp)
}

android {
  namespace = "com.trm.sightline.api.overpass"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

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
