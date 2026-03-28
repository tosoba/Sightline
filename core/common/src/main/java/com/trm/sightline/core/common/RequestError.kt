package com.trm.sightline.core.common

import java.io.IOException
import retrofit2.HttpException

sealed interface RequestError {
  data object IO : RequestError

  data class Http(val code: Int, val message: String) : RequestError

  data class Other(val throwable: Throwable) : RequestError
}

fun Throwable.toRequestError(): RequestError =
  when (this) {
    is IOException -> RequestError.IO
    is HttpException -> RequestError.Http(code(), message())
    else -> RequestError.Other(this)
  }

fun RequestError.messageResource(): Int =
  when (this) {
    RequestError.IO -> R.string.error_connection
    is RequestError.Http -> R.string.error_server
    is RequestError.Other -> R.string.error_unknown
  }
