import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kmp.library)
  alias(libs.plugins.metro)
}

kotlin {
  android {
    namespace = "com.trm.sightline.core.data"
    compileSdk {
      version = release(libs.versions.compileSdk.get().toInt()) {
        minorApiLevel = libs.versions.compileSdkMinor.get().toInt()
      }
    }
    minSdk = libs.versions.minSdk.get().toInt()

    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }

    withHostTest {}
  }

  iosArm64()
  iosSimulatorArm64()

  sourceSets {
    commonMain.dependencies {
      api(project(":api:photon"))
      api(project(":core:datastore"))
      api(project(":core:domain"))

      implementation(libs.kotlinx.coroutines.core)
    }

    commonTest.dependencies { implementation(kotlin("test")) }
  }
}
