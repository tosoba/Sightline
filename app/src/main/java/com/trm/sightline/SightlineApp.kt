package com.trm.sightline

import android.app.Application
import dev.zacsweers.metro.createGraphFactory

class SightlineApp : Application() {
  lateinit var appGraph: AppGraph
    private set

  override fun onCreate() {
    super.onCreate()
    appGraph = createGraphFactory<AppGraph.Factory>().create(this)
  }
}
