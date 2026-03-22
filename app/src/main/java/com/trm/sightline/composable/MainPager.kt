package com.trm.sightline.composable

import android.location.Location
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trm.sightline.MainPage
import com.trm.sightline.core.ar.model.RoundedRectF
import com.trm.sightline.core.ar.util.bottomSheetRectF
import com.trm.sightline.core.ar.util.collapsedBottomSheetContentHeightDp
import com.trm.sightline.core.ar.util.collapsedBottomSheetDragHandleHeightDp
import com.trm.sightline.core.ar.util.dpToPx
import com.trm.sightline.core.ar.util.sideSheetRectF
import com.trm.sightline.core.ar.util.sideSheetWidthDp
import com.trm.sightline.core.model.Place
import com.trm.sightline.feature.camera.CameraContent
import com.trm.sightline.feature.camera.CameraPreviewPagePicker
import com.trm.sightline.feature.map.MapPreview

@Composable
fun MainPager(
  pagerState: PagerState,
  location: Location?,
  places: List<Place>,
  isCompactHeight: Boolean,
  cameraPreviewBlurred: Boolean,
  cameraPreviewOverlayVisible: Boolean,
  onCameraPreviewTouch: () -> Unit,
) {
  HorizontalPager(
    state = pagerState,
    modifier = Modifier.fillMaxSize(),
    beyondViewportPageCount = 1,
    userScrollEnabled = false,
  ) { page ->
    when (MainPage.entries[page]) {
      MainPage.Camera -> {
        val context = LocalContext.current
        CameraContent(
          previewEnabled =
            pagerState.currentPage == MainPage.Camera.ordinal && !cameraPreviewBlurred,
          previewBlurred = cameraPreviewBlurred,
          location = location,
          places = places,
          blurredRectFs =
            listOf(
              RoundedRectF(
                rectF = if (isCompactHeight) context.sideSheetRectF else context.bottomSheetRectF,
                cornerRadius = if (isCompactHeight) 0f else context.dpToPx(64f),
              )
            ),
          onCameraPreviewTouch = onCameraPreviewTouch,
        ) { renderer ->
          val markersPagingState by renderer.markersPagingState.collectAsStateWithLifecycle()
          AnimatedVisibility(
            visible =
              cameraPreviewOverlayVisible &&
                !cameraPreviewBlurred &&
                markersPagingState.maxPage > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
              Modifier.align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(
                  start = 16.dp,
                  end = 16.dp + if (isCompactHeight) sideSheetWidthDp.dp else 0.dp,
                  bottom =
                    16.dp +
                      if (isCompactHeight) {
                        0.dp
                      } else {
                        (collapsedBottomSheetContentHeightDp +
                            collapsedBottomSheetDragHandleHeightDp)
                          .dp
                      },
                ),
          ) {
            CameraPreviewPagePicker(
              currentPage = markersPagingState.currentPage,
              totalPages = markersPagingState.maxPage + 1,
              onPageSelected = { renderer.currentPage = it },
            )
          }
        }
      }
      MainPage.Map -> {
        MapPreview(places = places, modifier = Modifier.fillMaxSize())
      }
    }
  }
}
