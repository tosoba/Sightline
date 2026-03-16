package com.trm.sightline.core.model

sealed interface LoadingState<out T> {
  data object Loading : LoadingState<Nothing>

  data class Loaded<out T>(val data: T) : LoadingState<T>
}
