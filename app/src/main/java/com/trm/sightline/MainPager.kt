package com.trm.sightline

import android.location.Location
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.trm.sightline.core.ar.model.RoundedRectF
import com.trm.sightline.core.ar.util.bottomSheetRectF
import com.trm.sightline.core.ar.util.dpToPx
import com.trm.sightline.core.ar.util.sideSheetRectF
import com.trm.sightline.core.model.Marker
import com.trm.sightline.feature.camera.CameraContent
import com.trm.sightline.feature.map.MapPreview

@Composable
fun MainPager(
  pagerState: PagerState,
  location: Location,
  markers: List<Marker>,
  isCompactHeight: Boolean,
) {
  val context = LocalContext.current
  HorizontalPager(
    state = pagerState,
    modifier = Modifier.fillMaxSize(),
    beyondViewportPageCount = 1,
    userScrollEnabled = false,
  ) { page ->
    when (MainPage.entries[page]) {
      MainPage.Camera -> {
        CameraContent(
          previewEnabled = pagerState.currentPage == MainPage.Camera.ordinal,
          location = location,
          markers = markers,
          blurredRectFs =
            listOf(
              RoundedRectF(
                rectF = if (isCompactHeight) context.sideSheetRectF else context.bottomSheetRectF,
                cornerRadius = if (isCompactHeight) 0f else context.dpToPx(64f),
              )
            ),
        )
      }
      MainPage.Map -> {
        MapPreview(markers = markers, modifier = Modifier.fillMaxSize())
      }
    }
  }
}
