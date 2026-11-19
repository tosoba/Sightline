package com.trm.sightline.core.common

fun RequestError.messageResource(): Int =
  when (this) {
    RequestError.IO -> R.string.error_connection
    is RequestError.Http -> R.string.error_server
    is RequestError.Other -> R.string.error_unknown
  }
