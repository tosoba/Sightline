package com.trm.sightline.core.common.location

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
@BindingContainer
object LocationModule {
  @Provides
  @SingleIn(AppScope::class)
  fun provideLocationProvider(): LocationProvider = IosLocationProvider()
}
