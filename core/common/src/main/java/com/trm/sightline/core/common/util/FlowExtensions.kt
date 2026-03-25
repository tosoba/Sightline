package com.trm.sightline.core.common.util

import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.cancellation.CancellationException

fun <A, B, R> Flow<A>.withLatestFrom(other: Flow<B>, transform: suspend (A, B) -> R): Flow<R> =
  flow {
    coroutineScope {
      val latestB = AtomicReference<B?>()
      val outerScope = this
      launch {
        try {
          other.collect { latestB.set(it) }
        } catch (ex: CancellationException) {
          outerScope.cancel(ex)
        }
      }
      collect { a: A -> latestB.get()?.let { b -> emit(transform(a, b)) } }
    }
  }
