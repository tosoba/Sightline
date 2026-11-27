import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kmp.library)
  alias(libs.plugins.kotlin.compose)
  id("kotlin-parcelize")
}

kotlin {
  android {
    namespace = "com.trm.sightline.core.ar"
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
    }

    commonTest.dependencies { implementation(kotlin("test")) }

    androidMain.dependencies {
      implementation(project(":core:ar-native"))
      implementation(project(":core:common"))
      implementation(project(":core:resources"))
      implementation(project(":core:ui"))

      implementation(libs.androidx.core.ktx)

      implementation(project.dependencies.platform(libs.androidx.compose.bom))
      implementation(libs.androidx.compose.ui)
      implementation(libs.androidx.compose.ui.graphics)

      implementation(libs.androidx.camera.camera2)
      implementation(libs.androidx.camera.lifecycle)
      implementation(libs.androidx.camera.view)
      implementation(libs.androidx.concurrent.futures.ktx)

      implementation(libs.kotlinx.coroutines.core)

      implementation(libs.timber)
    }
  }
}

composeCompiler { targetKotlinPlatforms.set(setOf(KotlinPlatformType.androidJvm)) }
