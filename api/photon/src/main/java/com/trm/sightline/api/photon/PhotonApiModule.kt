package com.trm.sightline.api.photon

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

@ContributesTo(AppScope::class)
@BindingContainer
object PhotonApiModule {
  @Provides
  @SingleIn(AppScope::class)
  fun providePhotonApi(
    client: OkHttpClient,
    loggingInterceptor: HttpLoggingInterceptor,
  ): PhotonApi =
    PhotonApi.create(
      if (BuildConfig.DEBUG) {
        client.newBuilder().addInterceptor(loggingInterceptor).build()
      } else {
        client
      }
    )
}
