import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kmp.library)
  alias(libs.plugins.kotlin.serialization)
}

kotlin {
  android {
    namespace = "com.trm.sightline.core.model"
    compileSdk = 37
    minSdk = libs.versions.minSdk.get().toInt()

    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }

    withHostTest {}
  }

  iosArm64()
  iosSimulatorArm64()

  // Transitional: core:datastore and core:domain are still JVM-only modules until their own
  // KMP conversions land (tickets 16 and 17).
  jvm()

  sourceSets {
    commonMain.dependencies { api(libs.kotlinx.serialization.json) }

    commonTest.dependencies { implementation(kotlin("test")) }
  }
}
