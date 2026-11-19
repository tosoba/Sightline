package com.trm.sightline.core.common

import io.ktor.client.plugins.ResponseException
import kotlinx.io.IOException

sealed interface RequestError {
  data object IO : RequestError

  data class Http(val code: Int, val message: String) : RequestError

  data class Other(val throwable: Throwable) : RequestError
}

fun Throwable.toRequestError(): RequestError =
  when (this) {
    is ResponseException -> RequestError.Http(response.status.value, response.status.description)
    is IOException -> RequestError.IO
    else -> RequestError.Other(this)
  }
