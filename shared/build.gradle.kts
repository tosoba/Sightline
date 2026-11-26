import co.touchlab.skie.configuration.FlowInterop
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.metro)
  alias(libs.plugins.skie)
}

skie {
  analytics {
    enabled.set(false)
  }

  features {
    coroutinesInterop.set(true)

    group {
      FlowInterop.Enabled(true)
    }
  }
}

kotlin {
  val xcframework = XCFramework("Shared")

  listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
    target.binaries.framework {
      baseName = "Shared"
      isStatic = true
      binaryOption("bundleId", "com.trm.sightline.shared")

      export(project(":core:model"))
      export(project(":core:common"))
      export(project(":core:ar"))
      export(project(":core:domain"))

      xcframework.add(this)
    }
  }

  sourceSets {
    commonMain.dependencies {
      api(project(":core:common"))
      api(project(":core:model"))
      api(project(":core:ar"))

      api(project(":core:data"))
      api(project(":core:domain"))

      api(libs.kotlinx.coroutines.core)
    }

    commonTest.dependencies {
      implementation(kotlin("test"))
      implementation(libs.kotlinx.coroutines.test)
    }
  }
}
