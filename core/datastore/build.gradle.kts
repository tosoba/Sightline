import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("java-library")
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.protobuf)
}

dependencies {
  api(project(":core:domain"))
  api(project(":core:model"))

  implementation(libs.androidx.datastore)
  implementation(libs.protobuf.kotlin.lite)

  implementation(libs.kotlinx.coroutines.core)
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_11 } }

protobuf {
  protoc { artifact = libs.protobuf.protoc.get().toString() }
  generateProtoTasks {
    all().configureEach {
      builtins {
        named("java") { option("lite") }
        register("kotlin") { option("lite") }
      }
    }
  }
}
