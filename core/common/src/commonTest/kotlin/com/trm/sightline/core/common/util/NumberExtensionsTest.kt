package com.trm.sightline.core.common.util

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberExtensionsTest {
  @Test
  fun `formats metres below one kilometre without decimals`() {
    assertEquals("950 m", 950f.formattedDistance())
    assertEquals("0 m", 0.4f.formattedDistance())
  }

  @Test
  fun `formats whole kilometres without a decimal part`() {
    assertEquals("1 km", 1_000f.formattedDistance())
    assertEquals("5 km", 5_000f.formattedDistance())
  }

  @Test
  fun `formats fractional kilometres with one decimal place`() {
    assertEquals("1.5 km", 1_500f.formattedDistance())
    assertEquals("2.5 km", 2_500f.formattedDistance())
    assertEquals("1.2 km", 1_234f.formattedDistance())
  }

  @Test
  fun `rounds to the requested number of decimal places`() {
    assertEquals(1.2, 1.2345.roundToDecimalPlaces(1), 0.0001)
    assertEquals(2.0, 1.95.roundToDecimalPlaces(1), 0.0001)
    assertEquals(3.0, 2.5.roundToDecimalPlaces(0), 0.0001)
  }
}
