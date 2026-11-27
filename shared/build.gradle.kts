import co.touchlab.skie.configuration.FlowInterop
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.metro)
  alias(libs.plugins.multiplatform.resources)
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
      export(project(":core:resources"))
      export("dev.icerock.moko:resources:${libs.versions.mokoResources.get()}")
      export("dev.icerock.moko:graphics:0.10.2")

      xcframework.add(this)
    }
  }

  sourceSets {
    iosMain.dependencies {
      api(project(":core:common"))
      api(project(":core:model"))
      api(project(":core:ar"))
      api(project(":core:resources"))

      api(project(":core:data"))
      api(project(":core:domain"))

      api(libs.kotlinx.coroutines.core)
    }

    iosTest.dependencies {
      implementation(kotlin("test"))
      implementation(libs.kotlinx.coroutines.test)
    }
  }
}

multiplatformResources {
  resourcesPackage.set("com.trm.sightline.shared.resources")
}

// The copy task writes into the app bundle Xcode is assembling, which lives outside this project,
// so Gradle cannot see that the destination changed and would skip the copy as up-to-date.
tasks.matching { it.name == "copyFrameworkResourcesToApp" }.configureEach {
  outputs.upToDateWhen { false }
}
