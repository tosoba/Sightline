package com.trm.sightline.core.common.di

import com.trm.sightline.core.common.network.configureSightlineClient
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

@OptIn(ExperimentalNativeApi::class)
@ContributesTo(AppScope::class)
@BindingContainer
object NetworkModule {
  @Provides
  @SingleIn(AppScope::class)
  fun provideHttpClient(): HttpClient =
    HttpClient(Darwin) { configureSightlineClient(logRequests = Platform.isDebugBinary) }
}
