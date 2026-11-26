package com.trm.sightline.core.common.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json

internal fun HttpClientConfig<*>.configureSightlineClient(logRequests: Boolean) {
  expectSuccess = true

  install(ContentNegotiation) {
    json(
      Json {
        ignoreUnknownKeys = true
        explicitNulls = false
      },
    )
  }

  install(HttpTimeout) {
    requestTimeoutMillis = 40.seconds.inWholeMilliseconds
    connectTimeoutMillis = 30.seconds.inWholeMilliseconds
    socketTimeoutMillis = 40.seconds.inWholeMilliseconds
  }

  if (logRequests) {
    install(Logging) { level = LogLevel.ALL }
  }
}
