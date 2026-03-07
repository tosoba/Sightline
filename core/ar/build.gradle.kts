plugins {
  alias(libs.plugins.android.library)
  id("kotlin-parcelize")
}

android {
  namespace = "com.trm.sightline.core.ar"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")

    externalNativeBuild {
      cmake {
        cppFlags("-std=c++17")
        arguments("-DCMAKE_VERBOSE_MAKEFILE=ON")
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  externalNativeBuild {
    cmake {
      path = File("src/main/cpp/CMakeLists.txt")
      version = "4.1.2"
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)

  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.concurrent.futures.ktx)

  implementation(libs.timber)
}
