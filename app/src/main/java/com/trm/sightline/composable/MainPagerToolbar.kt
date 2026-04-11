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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarVerticalFabPosition
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.trm.sightline.MainPage
import com.trm.sightline.R
import com.trm.sightline.core.ar.util.collapsedBottomSheetContentHeightDp
import com.trm.sightline.core.ar.util.collapsedBottomSheetDragHandleHeightDp
import com.trm.sightline.core.common.util.formattedDistance
import com.trm.sightline.core.model.PlaceSearchRadius

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BoxScope.MainPagerToolbar(
  visible: Boolean,
  isCompactHeight: Boolean,
  selectedPage: MainPage,
  searchRadius: PlaceSearchRadius,
  onPageSelected: (MainPage) -> Unit,
  onSearchRadiusChange: (PlaceSearchRadius) -> Unit,
) {
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
      MainPage.entries.forEach { page ->
        MainPagerToolbarItem(icon = page.icon, isSelected = selectedPage == page) {
          onPageSelected(page)
        }
      }
    }
  }
}

@Composable
private fun MainPagerToolbarItem(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
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
    }
  }
}

@Composable
private fun SettingsFloatingActionButton(
  searchRadius: PlaceSearchRadius,
  onSearchRadiusChange: (PlaceSearchRadius) -> Unit,
) {
  var menuExpanded by remember { mutableStateOf(false) }

  Box {
    FloatingActionButton(onClick = { menuExpanded = !menuExpanded }) {
      Icon(painter = painterResource(R.drawable.outline_distance_24), contentDescription = null)
    }

    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
      DropdownMenuItem(text = { Text("Select max range:") }, enabled = false, onClick = {})

      PlaceSearchRadius.entries.forEach { radius ->
        DropdownMenuItem(
          text = { Text(text = radius.meters.toFloat().formattedDistance()) },
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
