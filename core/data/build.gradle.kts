plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.metro)
}

android {
  namespace = "com.trm.sightline.core.data"
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
  api(project(":api:photon"))
  api(project(":core:datastore"))
  api(project(":core:domain"))

  api(libs.androidx.datastore)
  implementation(libs.androidx.datastore.preferences)

  implementation(libs.kotlinx.coroutines.core)

  implementation(libs.protobuf.kotlin.lite)

  testImplementation(libs.junit)
}
