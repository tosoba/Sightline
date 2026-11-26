package com.trm.sightline.core.data

import kotlin.test.Test
import kotlin.test.assertEquals

class RadiusConversionTest {
  @Test
  fun `converts metres to kilometres`() {
    assertEquals(1.5, metresToKilometres(1500f), 0.0001)
    assertEquals(0.5, metresToKilometres(500f), 0.0001)
    assertEquals(5.0, metresToKilometres(5000f), 0.0001)
  }

  @Test
  fun `handles fractional metre values`() {
    assertEquals(0.25, metresToKilometres(250f), 0.0001)
    assertEquals(0.001, metresToKilometres(1f), 0.0001)
  }
}
