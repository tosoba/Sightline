package com.trm.sightline.shared

import com.trm.sightline.core.data.UserPreferencesDirectory
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
internal fun userPreferencesDirectory(): UserPreferencesDirectory {
  val applicationSupport =
    NSSearchPathForDirectoriesInDomains(
        NSApplicationSupportDirectory,
        NSUserDomainMask,
        true,
      )
      .firstOrNull() as? String ?: error("No application support directory available")

  val directory = "$applicationSupport/Sightline"
  NSFileManager.defaultManager.createDirectoryAtPath(
    path = directory,
    withIntermediateDirectories = true,
    attributes = null,
    error = null,
  )

  return UserPreferencesDirectory(directory)
}
