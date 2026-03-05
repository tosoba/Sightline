package com.trm.sightline

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ARPageControl(currentPage: Int = 0, totalPages: Int = 10, onPageSelected: (Int) -> Unit) {
  val visibleIndices =
    remember(currentPage, totalPages) {
      when {
        totalPages <= 3 -> (0 until totalPages).toList()
        currentPage == 0 -> listOf(0, 1, 2)
        currentPage >= totalPages - 1 -> listOf(totalPages - 3, totalPages - 2, totalPages - 1)
        else -> listOf(currentPage - 1, currentPage, currentPage + 1)
      }
    }

  Surface(
    shape = CircleShape,
    color = Color.Black.copy(alpha = 0.5f),
    modifier = Modifier.padding(16.dp),
  ) {
    Box(contentAlignment = Alignment.CenterStart) {
      val selectedPosition = visibleIndices.indexOf(currentPage)
      val thumbOffset by
        animateFloatAsState(
          targetValue = selectedPosition * 48f,
          animationSpec = spring(stiffness = Spring.StiffnessLow),
          label = "ThumbScroll",
        )

      Box(
        modifier =
          Modifier.offset(x = thumbOffset.dp)
            .size(48.dp)
            .padding(4.dp)
            .background(Color.White, CircleShape)
      )

      Row(verticalAlignment = Alignment.CenterVertically) {
        visibleIndices.forEach { index ->
          Box(
            modifier =
              Modifier.size(48.dp).clickable(
                interactionSource = remember(::MutableInteractionSource),
                indication = null,
              ) {
                onPageSelected(index)
              },
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = (index + 1).toString(),
              color = if (currentPage == index) Color.Black else Color.White,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }
}

@Preview
@Composable
private fun ARPageControlPreview() {
  ARPageControl(onPageSelected = {})
}
