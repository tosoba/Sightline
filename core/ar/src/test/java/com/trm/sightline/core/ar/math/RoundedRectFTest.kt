package com.trm.sightline.core.ar.math

import kotlin.test.Test
import kotlin.test.assertEquals

class RoundedRectFTest {
  @Test
  fun `RoundedRectF keeps the bounds and the corner radius`() {
    val rect = RoundedRectF(left = 10f, top = 20f, right = 110f, bottom = 70f, cornerRadius = 16f)

    assertEquals(10f, rect.left)
    assertEquals(20f, rect.top)
    assertEquals(110f, rect.right)
    assertEquals(70f, rect.bottom)
    assertEquals(16f, rect.cornerRadius)
  }

  @Test
  fun `RoundedRectF derives width and height from the bounds`() {
    val rect = RoundedRectF(left = 10f, top = 20f, right = 110f, bottom = 70f, cornerRadius = 16f)

    assertEquals(100f, rect.width)
    assertEquals(50f, rect.height)
  }
}
