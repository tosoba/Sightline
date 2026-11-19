package com.trm.sightline.core.model

sealed interface LoadingState<out T : Any> {
  val data: T?

  data class Loading<T : Any>(override val data: T? = null) : LoadingState<T>

  data class Loaded<T : Any>(override val data: T) : LoadingState<T>
}
