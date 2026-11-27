import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kmp.library)
  alias(libs.plugins.multiplatform.resources)
}

kotlin {
  android {
    namespace = "com.trm.sightline.core.resources"
    compileSdk {
      version =
        release(libs.versions.compileSdk.get().toInt()) {
          minorApiLevel = libs.versions.compileSdkMinor.get().toInt()
        }
    }
    minSdk = libs.versions.minSdk.get().toInt()

    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }

    withHostTest { isIncludeAndroidResources = true }
  }

  iosArm64()
  iosSimulatorArm64()

  sourceSets {
    commonMain.dependencies {
      api(libs.moko.resources)
      api(project(":core:common"))
      api(project(":core:model"))
    }

    commonTest.dependencies { implementation(kotlin("test")) }
  }
}

multiplatformResources {
  resourcesPackage.set("com.trm.sightline.core.resources")
}
