package com.trm.sightline.core.ar.layout

/**
 * The sliding page window shown by both AR Marker page pickers: at most [maxVisiblePages] page
 * indices, centered on the current page and clamped to the start or end of the pager.
 */
object MarkerPageWindow {
  fun compute(
    currentPage: Int,
    totalPages: Int,
    maxVisiblePages: Int = 3,
  ): List<Int> {
    if (totalPages <= 0 || maxVisiblePages <= 0) return emptyList()

    val visible = minOf(totalPages, maxVisiblePages)
    if (visible == totalPages) return (0 until totalPages).toList()

    val page = currentPage.coerceIn(0, totalPages - 1)
    val start = (page - visible / 2).coerceIn(0, totalPages - visible)
    return (start until start + visible).toList()
  }
}
