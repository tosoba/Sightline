package com.trm.sightline.core.resources

import com.trm.sightline.core.common.RequestError
import com.trm.sightline.core.model.PlaceCategory
import kotlin.test.Test
import kotlin.test.assertEquals

class ResourceMappingsTest {
  @Test
  fun `every place category maps to a distinct label and icon`() {
    val categories = PlaceCategory.entries
    assertEquals(categories.size, categories.map { it.label }.distinct().size)
    assertEquals(categories.size, categories.map { it.icon }.distinct().size)
  }

  @Test
  fun `every request error variant maps to a distinct message`() {
    val messages =
      listOf(
        RequestError.IO.message,
        RequestError.Http(503, "Service Unavailable").message,
        RequestError.Other(IllegalStateException("boom")).message,
      )
    assertEquals(3, messages.distinct().size)
  }
}
