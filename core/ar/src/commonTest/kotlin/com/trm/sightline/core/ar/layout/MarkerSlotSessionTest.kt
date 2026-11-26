package com.trm.sightline.core.ar.layout

import com.trm.sightline.core.ar.math.MarkerSlotTarget
import com.trm.sightline.core.ar.math.ScreenRotation
import com.trm.sightline.core.model.GeoLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarkerSlotSessionTest {
  private val pov = GeoLocation(latitude = 52.52, longitude = 13.405)

  private val geometry =
    MarkerLayoutGeometry(
      markerWidth = 400f,
      markerHeight = 200f,
      rows = 2,
      divisor = 2,
      baseY = 300f,
      overlapGuard = 200f,
      verticalSpacing = 50f,
    )

  private fun north(meters: Double): GeoLocation =
    GeoLocation(latitude = pov.latitude + meters / METERS_PER_DEGREE, longitude = pov.longitude)

  private fun target(id: String, meters: Double): MarkerSlotTarget =
    MarkerSlotTarget(id = id, location = north(meters), distance = meters.toFloat())

  private val sixTargets =
    listOf(
      target("a", 10.0),
      target("b", 20.0),
      target("c", 30.0),
      target("d", 40.0),
      target("e", 50.0),
      target("f", 60.0),
    )

  private fun MarkerSlotSession.update(
    targets: List<MarkerSlotTarget> = sixTargets,
    location: GeoLocation = pov,
    geometry: MarkerLayoutGeometry = this@MarkerSlotSessionTest.geometry,
    screenRotation: ScreenRotation = ScreenRotation.ROTATION_0,
    canvasWidth: Int = 1080,
    canvasHeight: Int = 2400,
    previouslyVisibleIds: Set<String> = emptySet(),
  ): MarkerSlotUpdate =
    update(
      povLocation = location,
      targets = targets,
      geometry = geometry,
      screenRotation = screenRotation,
      canvasWidth = canvasWidth,
      canvasHeight = canvasHeight,
      previouslyVisibleIds = previouslyVisibleIds,
    )

  @Test
  fun `assigns slots and exposes the number of pages`() {
    val update = MarkerSlotSession().update()

    assertTrue(update.didReassign)
    assertEquals(0, update.currentPage)
    assertEquals(3, update.totalPages)
    assertEquals(0, update.slots.getValue("a").page)
    assertEquals(0, update.slots.getValue("b").page)
    assertEquals(1, update.slots.getValue("c").page)
    assertEquals(1, update.slots.getValue("d").page)
    assertEquals(2, update.slots.getValue("e").page)
    assertEquals(2, update.slots.getValue("f").page)
  }

  @Test
  fun `reuses cached slots when the active location moved less than ten meters`() {
    val session = MarkerSlotSession()
    val first = session.update()

    val second = session.update(location = north(9.0))

    assertFalse(second.didReassign)
    assertEquals(first.slots, second.slots)
  }

  @Test
  fun `reassigns once the active location moved at least ten meters`() {
    val session = MarkerSlotSession()
    session.update()

    val moved = session.update(location = north(11.0))

    assertTrue(moved.didReassign)
  }

  @Test
  fun `reassigns when the place id list changes`() {
    val session = MarkerSlotSession()
    session.update()

    val added = session.update(targets = sixTargets + target("g", 70.0))

    assertTrue(added.didReassign)
  }

  @Test
  fun `reassigns when the layout geometry changes`() {
    val session = MarkerSlotSession()
    session.update()

    val resized = session.update(geometry = geometry.copy(markerWidth = 420f))

    assertTrue(resized.didReassign)
  }

  @Test
  fun `reassigns when the screen rotation changes`() {
    val session = MarkerSlotSession()
    session.update()

    val rotated = session.update(screenRotation = ScreenRotation.ROTATION_90)

    assertTrue(rotated.didReassign)
  }

  @Test
  fun `reassigns when the canvas changes size`() {
    val session = MarkerSlotSession()
    session.update()

    val resized = session.update(canvasWidth = 2400, canvasHeight = 1080)

    assertTrue(resized.didReassign)
  }

  @Test
  fun `follows previously visible places when a reassignment moves them to another page`() {
    val session = MarkerSlotSession()
    session.update()
    session.selectPage(0)

    val update =
      session.update(
        targets = listOf(target("g", 1.0), target("h", 2.0)) + sixTargets,
        screenRotation = ScreenRotation.ROTATION_90,
        previouslyVisibleIds = setOf("a", "b"),
      )

    assertEquals(1, update.currentPage)
  }

  @Test
  fun `follows the lowest page when previously visible places are spread evenly`() {
    val session = MarkerSlotSession()
    session.update()
    session.selectPage(1)

    val update =
      session.update(
        screenRotation = ScreenRotation.ROTATION_90,
        previouslyVisibleIds = setOf("c", "e"),
      )

    assertEquals(1, update.currentPage)
  }

  @Test
  fun `keeps the current page when no previously visible place survived`() {
    val session = MarkerSlotSession()
    session.update()
    session.selectPage(2)

    val update =
      session.update(
        targets =
          listOf("p", "q", "r", "s", "t", "u").mapIndexed { index, id ->
            target(id, 10.0 + index)
          },
        screenRotation = ScreenRotation.ROTATION_90,
        previouslyVisibleIds = setOf("a", "b"),
      )

    assertEquals(2, update.currentPage)
  }

  @Test
  fun `clamps the current page when the page count shrinks`() {
    val session = MarkerSlotSession()
    session.update()
    session.selectPage(2)

    val shrunk = session.update(targets = sixTargets.take(3))

    assertEquals(2, shrunk.totalPages)
    assertEquals(1, shrunk.currentPage)
  }

  @Test
  fun `selecting a page clamps it into the pager`() {
    val session = MarkerSlotSession()
    session.update()

    assertTrue(session.selectPage(5))
    assertEquals(2, session.currentPage)

    assertTrue(session.selectPage(-1))
    assertEquals(0, session.currentPage)

    assertFalse(session.selectPage(0))
  }

  @Test
  fun `an empty place list collapses to a single page`() {
    val session = MarkerSlotSession()
    session.update()
    session.selectPage(2)

    val empty = session.update(targets = emptyList())

    assertTrue(empty.slots.isEmpty())
    assertEquals(1, empty.totalPages)
    assertEquals(0, empty.currentPage)
  }

  @Test
  fun `does not assign before the canvas is measured`() {
    val session = MarkerSlotSession()

    val unmeasured = session.update(canvasWidth = 0, canvasHeight = 0)

    assertFalse(unmeasured.didReassign)
    assertTrue(unmeasured.slots.isEmpty())
    assertEquals(1, unmeasured.totalPages)

    val measured = session.update()
    assertTrue(measured.didReassign)
    assertEquals(3, measured.totalPages)
  }

  private companion object {
    const val METERS_PER_DEGREE = 111_194.9
  }
}
