package com.trm.sightline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainPagerToolbar(
  isCompactHeight: Boolean,
  selectedPage: MainPage,
  onPageSelected: (MainPage) -> Unit,
  modifier: Modifier = Modifier,
) {
  val toolbarContent =
    @Composable { showLabel: Boolean ->
      MainPage.entries.forEach { page ->
        MainPagerToolbarItem(
          label = page.label,
          icon = page.icon,
          isSelected = selectedPage == page,
          showLabel = showLabel,
        ) {
          onPageSelected(page)
        }
      }
    }
  if (isCompactHeight) {
    VerticalFloatingToolbar(
      expanded = true,
      modifier =
        modifier
          .windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Bottom)
          )
          .padding(16.dp),
      content = { toolbarContent(false) },
    )
  } else {
    HorizontalFloatingToolbar(
      expanded = true,
      modifier =
        modifier
          .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
          .padding(16.dp),
      content = { toolbarContent(true) },
    )
  }
}

@Composable
private fun MainPagerToolbarItem(
  label: String,
  icon: ImageVector,
  isSelected: Boolean,
  showLabel: Boolean = true,
  onClick: () -> Unit,
) {
  Surface(
    selected = isSelected,
    onClick = onClick,
    shape = CircleShape,
    color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
    contentColor =
      if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
      else MaterialTheme.colorScheme.onSurfaceVariant,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))

      if (isSelected && showLabel) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
      }
    }
  }
}
