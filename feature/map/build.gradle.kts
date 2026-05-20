plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.trm.sightline.feature.map"
  compileSdk { version = release(37) {} }

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

  buildFeatures { compose = true }
}

dependencies {
  implementation(project(":core:ar"))
  implementation(project(":core:ui"))
  implementation(project(":core:model"))

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)

  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  debugImplementation(libs.androidx.compose.ui.tooling)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)

  debugImplementation(libs.maplibre.compose.get().toString()) {
    exclude(group = "org.maplibre.gl", module = "android-sdk")
  }
  releaseImplementation(libs.maplibre.compose)
  debugImplementation(libs.maplibre.android.opengl)

  implementation(libs.material)

  implementation(libs.timber)
}
