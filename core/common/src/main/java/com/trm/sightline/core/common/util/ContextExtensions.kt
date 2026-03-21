package com.trm.sightline.core.common.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

fun Context.startAppSettingsActivity() {
  startActivity(
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
      .setData(Uri.fromParts("package", packageName, null))
  )
}
