import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kmp.library)
  alias(libs.plugins.kotlin.serialization)
}

kotlin {
  android {
    namespace = "com.trm.sightline.core.datastore"
    compileSdk = 37
    minSdk = libs.versions.minSdk.get().toInt()

    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }

    withHostTest {}
  }

  iosArm64()
  iosSimulatorArm64()

  sourceSets {
    commonMain.dependencies {
      api(project(":core:model"))
      api(libs.androidx.datastore.core)
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.kotlinx.serialization.json)
    }

    iosMain.dependencies {
      implementation(libs.androidx.datastore.core.okio)
      implementation(libs.okio)
    }

    commonTest.dependencies { implementation(kotlin("test")) }
  }
}
