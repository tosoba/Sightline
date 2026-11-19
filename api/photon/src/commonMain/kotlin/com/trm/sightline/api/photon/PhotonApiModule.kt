package com.trm.sightline.api.photon

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient

@ContributesTo(AppScope::class)
@BindingContainer
object PhotonApiModule {
  @Provides
  @SingleIn(AppScope::class)
  fun providePhotonApi(client: HttpClient): PhotonApi = PhotonApi(client)
}
