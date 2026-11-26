import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kmp.library)
  alias(libs.plugins.kotlin.serialization)
}

kotlin {
  android {
    namespace = "com.trm.sightline.core.model"
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
    commonMain.dependencies { api(libs.kotlinx.serialization.json) }

    commonTest.dependencies { implementation(kotlin("test")) }
  }
}
