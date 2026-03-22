import java.util.Properties

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.hilt)
  alias(libs.plugins.ksp)
}

android {
  namespace = "com.trm.sightline.api.nominatim"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  val properties = Properties()
  val localPropertiesFile = project.rootProject.file("local.properties")
  if (localPropertiesFile.exists()) {
    properties.load(localPropertiesFile.inputStream())
  }

  val nominatimUserAgent =
    properties.getProperty("nominatim.useragent")
      ?: throw GradleException(
        "Required property 'nominatim.useragent' not found in local.properties file."
      )

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    buildConfigField("String", "NOMINATIM_USER_AGENT", "\"$nominatimUserAgent\"")
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
  implementation(libs.okhttp)
  implementation(libs.moshi)
  implementation(libs.moshi.adapters)
  api(libs.retrofit)
  implementation(libs.retrofit.converter.moshi)
  implementation(libs.kotlinx.coroutines.core)

  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  ksp(libs.moshi.kotlin.codegen)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
