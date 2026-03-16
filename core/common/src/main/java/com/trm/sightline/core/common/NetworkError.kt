package com.trm.sightline.core.common

import retrofit2.HttpException
import java.io.IOException

sealed interface NetworkError {
  data object IO : NetworkError
  data class Http(val code: Int, val message: String) : NetworkError
  data class Other(val throwable: Throwable) : NetworkError
}

fun Throwable.toNetworkError(): NetworkError {
  return when (this) {
    is IOException -> NetworkError.IO
    is HttpException -> NetworkError.Http(code(), message())
    else -> NetworkError.Other(this)
  }
}
