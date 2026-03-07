plugins { id("java-library") }

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
  implementation(libs.okhttp)
  implementation(libs.moshi)
  implementation(libs.moshi.adapters)
  implementation(libs.retrofit)
  implementation(libs.retrofit.converter.moshi)
}
