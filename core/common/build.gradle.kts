import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kmp.library)
  alias(libs.plugins.metro)
}

kotlin {
  android {
    namespace = "com.trm.sightline.core.common"
    compileSdk {
      version =
        release(libs.versions.compileSdk.get().toInt()) {
          minorApiLevel = libs.versions.compileSdkMinor.get().toInt()
        }
    }
    minSdk = libs.versions.minSdk.get().toInt()

    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }

    withHostTest {}

    androidResources { enable = true }
  }

  iosArm64()
  iosSimulatorArm64()

  sourceSets {
    commonMain.dependencies {
      api(project(":core:model"))

      api(libs.kotlinx.coroutines.core)
      api(libs.ktor.client.core)
      implementation(libs.kotlinx.serialization.json)
      implementation(libs.ktor.client.content.negotiation)
      implementation(libs.ktor.serialization.kotlinx.json)
    }

    commonTest.dependencies { implementation(kotlin("test")) }

    androidMain.dependencies {
      implementation(libs.androidx.activity.compose)

      implementation(libs.play.services.location)

      implementation(libs.ktor.client.okhttp)
      implementation(libs.ktor.client.logging)
      implementation(libs.okhttp)
    }

    iosMain.dependencies {
      implementation(libs.ktor.client.darwin)
      implementation(libs.ktor.client.logging)
    }
  }
}
