import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kmp.library)
}

kotlin {
  android {
    namespace = "com.trm.sightline.core.domain"
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
      api(project(":core:model"))
      api(project(":core:common"))
      implementation(libs.kotlinx.coroutines.core)
    }

    commonTest.dependencies {
      implementation(kotlin("test"))
      implementation(libs.kotlinx.coroutines.test)
    }
  }
}
