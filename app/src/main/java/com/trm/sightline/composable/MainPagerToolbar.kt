package com.trm.sightline.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarHorizontalFabPosition
import androidx.compose.material3.FloatingToolbarVerticalFabPosition
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.trm.sightline.MainPage
import com.trm.sightline.core.ar.util.collapsedBottomSheetContentHeightDp
import com.trm.sightline.core.ar.util.collapsedBottomSheetDragHandleHeightDp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BoxScope.MainPagerToolbar(
  visible: Boolean,
  isCompactHeight: Boolean,
  selectedPage: MainPage,
  searchRadius: Int,
  onPageSelected: (MainPage) -> Unit,
  onSearchRadiusChange: (Int) -> Unit,
) {
  val toolbarContent =
    @Composable { showLabel: Boolean ->
      MainPage.entries.forEach { page ->
        MainPagerToolbarItem(
          label = stringResource(page.labelRes),
          icon = page.icon,
          isSelected = selectedPage == page,
          showLabel = showLabel,
        ) {
          onPageSelected(page)
        }
      }
    }
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(),
    exit = fadeOut(),
    modifier =
      if (isCompactHeight) {
        Modifier.align(Alignment.BottomStart)
          .windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Bottom)
          )
          .padding(16.dp)
      } else {
        Modifier.align(Alignment.BottomStart)
          .windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Bottom)
          )
          .padding(
            start = 16.dp,
            end = 16.dp,
            bottom =
              (16 + collapsedBottomSheetContentHeightDp + collapsedBottomSheetDragHandleHeightDp).dp,
          )
      },
  ) {
    if (isCompactHeight) {
      VerticalFloatingToolbar(
        expanded = true,
        floatingActionButtonPosition = FloatingToolbarVerticalFabPosition.Top,
        floatingActionButton = {
          SettingsFloatingActionButton(
            searchRadius = searchRadius,
            onSearchRadiusChange = onSearchRadiusChange,
          )
        },
      ) {
        toolbarContent(false)
      }
    } else {
      HorizontalFloatingToolbar(
        expanded = true,
        floatingActionButtonPosition = FloatingToolbarHorizontalFabPosition.Start,
        floatingActionButton = {
          SettingsFloatingActionButton(
            searchRadius = searchRadius,
            onSearchRadiusChange = onSearchRadiusChange,
          )
        },
      ) {
        toolbarContent(true)
      }
    }
  }
}

@Composable
private fun SettingsFloatingActionButton(searchRadius: Int, onSearchRadiusChange: (Int) -> Unit) {
  var menuExpanded by remember { mutableStateOf(false) }

  Box {
    FloatingActionButton(onClick = { menuExpanded = !menuExpanded }) {
      Icon(imageVector = Icons.Default.Settings, contentDescription = null)
    }

    DropdownMenu(
      expanded = menuExpanded,
      onDismissRequest = { menuExpanded = false },
      offset = DpOffset(x = 0.dp, y = (-16).dp),
    ) {
      listOf(500, 1000, 2000, 5000).forEach { radius ->
        DropdownMenuItem(
          text = {
            val label = if (radius >= 1000) "${radius / 1000}km" else "${radius}m"
            Text(label)
          },
          onClick = {
            onSearchRadiusChange(radius)
            menuExpanded = false
          },
          trailingIcon =
            if (searchRadius == radius) {
              { Icon(Icons.Default.Check, contentDescription = null) }
            } else {
              null
            },
        )
      }
    }
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
