package com.trm.sightline.core.ar.math

import com.trm.sightline.core.model.GeoLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkerSlotAssignmentTest {
  private val pov = GeoLocation(latitude = 52.52, longitude = 13.405)
  private val north = GeoLocation(latitude = 52.5202, longitude = 13.405)
  private val south = GeoLocation(latitude = 52.5198, longitude = 13.405)

  private val geometry =
    MarkerSlotGeometry(
      markerWidthPx = 400f,
      markerHeightPx = 200f,
      overlapGuardPx = 200f,
      verticalSpacingPx = 50f,
      rows = 3,
      baseY = 300f,
    )

  private fun target(id: String, location: GeoLocation, distance: Float) =
    MarkerSlotTarget(id = id, location = location, distance = distance)

  private fun assign(
    vararg targets: MarkerSlotTarget,
    canvasWidth: Int = 1080,
    canvasHeight: Int = 2400,
  ): Map<String, MarkerSlot> =
    MarkerSlotAssignment.assign(
      povLocation = pov,
      targets = targets.toList(),
      geometry = geometry,
      canvasWidth = canvasWidth,
      canvasHeight = canvasHeight,
    )

  @Test
  fun `assign stacks markers with the same bearing into rows and pages`() {
    val slots =
      assign(
        target("a", north, 10f),
        target("b", north, 20f),
        target("c", north, 30f),
        target("d", north, 40f),
        target("e", north, 50f),
      )

    assertEquals(300f, slots.getValue("a").y)
    assertEquals(0, slots.getValue("a").page)
    assertEquals(550f, slots.getValue("b").y)
    assertEquals(0, slots.getValue("b").page)
    assertEquals(800f, slots.getValue("c").y)
    assertEquals(0, slots.getValue("c").page)
    assertEquals(300f, slots.getValue("d").y)
    assertEquals(1, slots.getValue("d").page)
    assertEquals(550f, slots.getValue("e").y)
    assertEquals(1, slots.getValue("e").page)
  }

  @Test
  fun `assign fills free rows in increasing distance order`() {
    val slots =
      assign(
        target("far", north, 50f),
        target("near", north, 10f),
        target("middle", north, 30f),
      )

    assertEquals(300f, slots.getValue("near").y)
    assertEquals(550f, slots.getValue("middle").y)
    assertEquals(800f, slots.getValue("far").y)
  }

  @Test
  fun `assign gives opposite bearings the same slot`() {
    val slots = assign(target("a", north, 10f), target("b", south, 20f))

    assertEquals(300f, slots.getValue("a").y)
    assertEquals(0, slots.getValue("a").page)
    assertEquals(300f, slots.getValue("b").y)
    assertEquals(0, slots.getValue("b").page)
  }

  @Test
  fun `assign normalizes the bearing of every target`() {
    val slots = assign(target("north", north, 10f), target("south", south, 20f))

    assertEquals(0.0, slots.getValue("north").bearing.toDouble(), 1e-6)
    assertEquals(180.0, slots.getValue("south").bearing.toDouble(), 1e-6)
  }

  @Test
  fun `assign returns nothing without targets`() {
    assertTrue(assign().isEmpty())
  }

  @Test
  fun `assign returns nothing before the canvas is measured`() {
    val slots = assign(target("a", north, 10f), canvasWidth = 0, canvasHeight = 0)

    assertTrue(slots.isEmpty())
  }
}
