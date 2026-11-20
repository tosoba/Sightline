plugins { alias(libs.plugins.android.library) }

android {
  namespace = "com.trm.sightline.core.arnative"
  compileSdk = 37

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()

    externalNativeBuild {
      cmake {
        cppFlags("-std=c++17")
        arguments("-DCMAKE_VERBOSE_MAKEFILE=ON")
      }
    }
  }

  buildTypes {
    release { isMinifyEnabled = false }
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
