package com.trm.sightline.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trm.sightline.MainPage
import com.trm.sightline.R
import com.trm.sightline.core.ar.util.collapsedBottomSheetContentHeightDp
import com.trm.sightline.core.ar.util.collapsedBottomSheetDragHandleHeightDp
import com.trm.sightline.core.common.util.formattedDistance
import com.trm.sightline.core.model.PlaceSearchRadius

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
      Modifier.align(Alignment.BottomStart)
        .windowInsetsPadding(
          WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Bottom)
        )
        .padding(
          top = 16.dp,
          start = 16.dp,
          end = 16.dp,
          bottom =
            (16 +
                if (isCompactHeight) {
                  0
                } else {
                  collapsedBottomSheetContentHeightDp + collapsedBottomSheetDragHandleHeightDp
                })
              .dp,
        ),
  ) {
    Column(horizontalAlignment = Alignment.Start) {
      MainPagerPlaceSearchRadiusMenu(
        searchRadius = searchRadius,
        onSearchRadiusChange = onSearchRadiusChange,
      )

      VerticalFloatingToolbar(expanded = true) {
        MainPage.entries.forEach { page ->
          MainPagerToolbarItem(
            icon = page.icon,
            isSelected = selectedPage == page,
            onClick = { onPageSelected(page) },
          )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainPagerPlaceSearchRadiusMenu(
  searchRadius: PlaceSearchRadius,
  onSearchRadiusChange: (PlaceSearchRadius) -> Unit,
) {
  var menuExpanded by remember { mutableStateOf(false) }

  FloatingActionButtonMenu(
    expanded = menuExpanded,
    horizontalAlignment = Alignment.Start,
    modifier = Modifier.offset(x = (-4).dp),
    button = {
      TooltipBox(
        positionProvider =
          TooltipDefaults.rememberTooltipPositionProvider(
            if (menuExpanded) TooltipAnchorPosition.Start else TooltipAnchorPosition.Above
          ),
        tooltip = { PlainTooltip { Text(text = stringResource(R.string.select_max_range)) } },
        state = rememberTooltipState(),
      ) {
        ToggleFloatingActionButton(
          checked = menuExpanded,
          onCheckedChange = { menuExpanded = it },
        ) {
          Icon(painter = painterResource(R.drawable.outline_distance_24), contentDescription = null)
        }
      }
    },
  ) {
    PlaceSearchRadius.entries.forEach { radius ->
      FloatingActionButtonMenuItem(
        onClick = {
          onSearchRadiusChange(radius)
          menuExpanded = false
        },
        text = { Text(text = radius.meters.toFloat().formattedDistance()) },
        icon = {
          if (searchRadius == radius) {
            Icon(Icons.Default.Check, contentDescription = null)
          }
        },
      )
    }
  }
}
