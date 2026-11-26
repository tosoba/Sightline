package com.trm.sightline.core.ar.layout

import kotlin.test.Test
import kotlin.test.assertEquals

class MarkerPageWindowTest {
  @Test
  fun `no pages produce an empty window`() {
    assertEquals(emptyList(), MarkerPageWindow.compute(currentPage = 0, totalPages = 0))
  }

  @Test
  fun `a short pager shows every page`() {
    assertEquals(listOf(0), MarkerPageWindow.compute(currentPage = 0, totalPages = 1))
    assertEquals(listOf(0, 1, 2), MarkerPageWindow.compute(currentPage = 1, totalPages = 3))
  }

  @Test
  fun `the first page shows the leading window`() {
    assertEquals(listOf(0, 1, 2), MarkerPageWindow.compute(currentPage = 0, totalPages = 10))
  }

  @Test
  fun `a middle page shows its neighbours`() {
    assertEquals(listOf(4, 5, 6), MarkerPageWindow.compute(currentPage = 5, totalPages = 10))
  }

  @Test
  fun `the last pages show the trailing window`() {
    assertEquals(listOf(7, 8, 9), MarkerPageWindow.compute(currentPage = 9, totalPages = 10))
    assertEquals(listOf(7, 8, 9), MarkerPageWindow.compute(currentPage = 8, totalPages = 10))
  }

  @Test
  fun `an out-of-range page is clamped into the pager`() {
    assertEquals(listOf(0, 1, 2), MarkerPageWindow.compute(currentPage = -2, totalPages = 10))
    assertEquals(listOf(7, 8, 9), MarkerPageWindow.compute(currentPage = 42, totalPages = 10))
  }

  @Test
  fun `the window size is configurable`() {
    assertEquals(listOf(3, 4), MarkerPageWindow.compute(currentPage = 4, totalPages = 10, maxVisiblePages = 2))
    assertEquals(listOf(2, 3, 4, 5), MarkerPageWindow.compute(currentPage = 4, totalPages = 10, maxVisiblePages = 4))
  }
}
