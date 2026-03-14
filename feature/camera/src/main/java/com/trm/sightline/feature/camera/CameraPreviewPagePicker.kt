package com.trm.sightline.feature.camera

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CameraPreviewPagePicker(
  currentPage: Int,
  totalPages: Int,
  modifier: Modifier = Modifier,
  onPageSelected: (Int) -> Unit,
) {
  VerticalFloatingToolbar(expanded = true, modifier = modifier) {
    remember(currentPage, totalPages) {
        when {
          totalPages <= 3 -> (0 until totalPages).toList()
          currentPage == 0 -> listOf(0, 1, 2)
          currentPage >= totalPages - 1 -> listOf(totalPages - 3, totalPages - 2, totalPages - 1)
          else -> listOf(currentPage - 1, currentPage, currentPage + 1)
        }
      }
      .forEach { index ->
        CameraPreviewPagePickerItem(
          pageNumber = index + 1,
          isSelected = currentPage == index,
          onClick = { onPageSelected(index) },
        )
      }
  }
}

@Composable
private fun CameraPreviewPagePickerItem(pageNumber: Int, isSelected: Boolean, onClick: () -> Unit) {
  Surface(
    selected = isSelected,
    onClick = onClick,
    shape = CircleShape,
    color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
    contentColor =
      if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
      else MaterialTheme.colorScheme.onSurfaceVariant,
  ) {
    Text(
      text = pageNumber.toString(),
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    )
  }
}

@Preview
@Composable
private fun CameraPreviewPagePickerPreview(
  @PreviewParameter(CameraPreviewPagePickerParamsProvider::class)
  params: CameraPreviewPagePickerParams
) {
  CameraPreviewPagePicker(
    currentPage = params.currentPage,
    totalPages = params.totalPages,
    onPageSelected = {},
  )
}

private data class CameraPreviewPagePickerParams(val currentPage: Int, val totalPages: Int)

private class CameraPreviewPagePickerParamsProvider :
  PreviewParameterProvider<CameraPreviewPagePickerParams> {
  override val values =
    sequenceOf(
      CameraPreviewPagePickerParams(currentPage = 0, totalPages = 2),
      CameraPreviewPagePickerParams(currentPage = 0, totalPages = 10),
      CameraPreviewPagePickerParams(currentPage = 9, totalPages = 10),
      CameraPreviewPagePickerParams(currentPage = 5, totalPages = 10),
    )
}
