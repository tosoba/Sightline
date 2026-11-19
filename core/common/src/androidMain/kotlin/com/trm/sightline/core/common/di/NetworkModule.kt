package com.trm.sightline.core.common.di

import android.content.Context
import android.content.pm.ApplicationInfo
import com.trm.sightline.core.common.network.configureSightlineClient
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.io.File
import okhttp3.Cache

@ContributesTo(AppScope::class)
@BindingContainer
object NetworkModule {
  @Provides
  @SingleIn(AppScope::class)
  fun provideCache(context: Context): Cache =
    Cache(File(context.cacheDir, "http_cache"), HTTP_CACHE_BYTES)

  @Provides
  @SingleIn(AppScope::class)
  fun provideHttpClient(cache: Cache, context: Context): HttpClient =
    HttpClient(OkHttp) {
      configureSightlineClient(logRequests = context.isDebuggable())
      engine { config { cache(cache) } }
    }

  private fun Context.isDebuggable(): Boolean =
    applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

  private const val HTTP_CACHE_BYTES = 10 * 1024 * 1024L
}
