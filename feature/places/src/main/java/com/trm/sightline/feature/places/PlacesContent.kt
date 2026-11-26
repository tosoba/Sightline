package com.trm.sightline.feature.places

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trm.sightline.core.domain.places.CategoryPlacesState
import com.trm.sightline.core.domain.places.CustomLocationSearchState
import com.trm.sightline.core.domain.places.LocationAddressState
import com.trm.sightline.core.model.CustomLocation
import com.trm.sightline.core.model.GeoLocation
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import com.trm.sightline.core.ui.icon
import kotlinx.coroutines.launch

@Composable
fun SharedTransitionScope.PlacesContent(
  places: Map<PlaceCategory, CategoryPlacesState>,
  userLocationEnabled: Boolean,
  activeLocation: GeoLocation?,
  userLocationAddress: LocationAddressState,
  customLocationAddress: String,
  customLocationSearchResults: CustomLocationSearchState,
  layout: PlacesLayout,
  alpha: Float,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier = Modifier,
  onSearchFocusChange: (Boolean) -> Unit,
  onToggleUserLocationEnabled: (Boolean) -> Unit,
  onCustomLocationAddressChange: (String) -> Unit,
  onCustomLocationSearchResultClick: (CustomLocation) -> Unit,
  onCancelCustomLocationSearch: () -> Unit,
  onTogglePlaceCategory: (PlaceCategory) -> Unit,
  onCategoryClick: (PlaceCategory, List<Place>) -> Unit,
) {
  val focusManager = LocalFocusManager.current
  val searchBarState = rememberSearchBarState()
  val scope = rememberCoroutineScope()
  val expanded = searchBarState.targetValue == SearchBarValue.Expanded
  val textFieldState = rememberTextFieldState(customLocationAddress)

  LaunchedEffect(customLocationAddress) {
    if (textFieldState.text.toString() != customLocationAddress) {
      textFieldState.setTextAndPlaceCursorAtEnd(customLocationAddress)
    }
  }

  LaunchedEffect(textFieldState, customLocationAddress) {
    snapshotFlow { textFieldState.text.toString() }
      .collect { query ->
        if (customLocationAddress != query) {
          onCustomLocationAddressChange(query)
        }
      }
  }

  LaunchedEffect(userLocationEnabled) {
    if (userLocationEnabled && expanded) {
      searchBarState.animateToCollapsed()
    }
  }
  LaunchedEffect(expanded) { onSearchFocusChange(expanded) }

  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    val searchBarColors =
      SearchBarDefaults.colors().run {
        copy(containerColor = containerColor.copy(alpha = alpha))
      }
    val inputField: @Composable () -> Unit = {
      SearchBarDefaults.InputField(
        modifier = Modifier.fillMaxWidth(),
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        onSearch = {},
        enabled = !userLocationEnabled,
        placeholder = {
          Text(
            stringResource(
              when {
                !userLocationEnabled -> R.string.enter_location_placeholder
                activeLocation == null -> R.string.loading_location
                userLocationAddress is LocationAddressState.Loading ->
                  R.string.loading_location_address
                else -> R.string.location_address_not_found
              },
            ),
          )
        },
        leadingIcon = {
          when {
            userLocationEnabled &&
              (userLocationAddress is LocationAddressState.Loading || activeLocation == null) -> {
              CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
            userLocationEnabled && userLocationAddress is LocationAddressState.Loaded -> {
              Icon(imageVector = Icons.Default.GpsFixed, contentDescription = null)
            }
            expanded -> {
              IconButton(
                onClick = {
                  focusManager.clearFocus()
                  onCancelCustomLocationSearch()
                  scope.launch { searchBarState.animateToCollapsed() }
                },
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = stringResource(R.string.back_content_description),
                )
              }
            }
            else -> {
              Icon(imageVector = Icons.Default.Search, contentDescription = null)
            }
          }
        },
        trailingIcon = {
          AnimatedVisibility(visible = !expanded) {
            FilledTonalIconToggleButton(
              checked = userLocationEnabled,
              onCheckedChange = onToggleUserLocationEnabled,
            ) {
              Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = stringResource(R.string.my_location_content_description),
              )
            }
          }
        },
      )
    }

    SearchBar(
      state = searchBarState,
      inputField = inputField,
      modifier = Modifier.fillMaxWidth(),
      colors = searchBarColors,
    )

    ExpandedFullScreenSearchBar(
      state = searchBarState,
      inputField = inputField,
    ) {
      LazyColumn {
        when (val results = customLocationSearchResults) {
          CustomLocationSearchState.Idle -> {
            item {
              Box(
                modifier = Modifier.fillParentMaxWidth().padding(16.dp).animateItem(),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = stringResource(R.string.start_typing_to_search),
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  textAlign = TextAlign.Center,
                )
              }
            }
          }
          CustomLocationSearchState.QueryTooShort -> {
            item {
              Box(
                modifier = Modifier.fillParentMaxWidth().padding(16.dp).animateItem(),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = stringResource(R.string.query_too_short),
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  textAlign = TextAlign.Center,
                )
              }
            }
          }
          CustomLocationSearchState.Loading -> {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth().animateItem()) }
          }
          is CustomLocationSearchState.Loaded -> {
            items(results.results) { result ->
              ListItem(
                leadingContent = {
                  Icon(imageVector = Icons.Default.Place, contentDescription = null)
                },
                colors =
                  ListItemDefaults.colors(
                    containerColor = ListItemDefaults.colors().containerColor.copy(alpha = 0f),
                  ),
                modifier =
                  Modifier.animateItem().clickable {
                    focusManager.clearFocus()
                    scope.launch { searchBarState.animateToCollapsed() }
                    onCustomLocationSearchResultClick(result)
                  },
              ) {
                Text(text = result.address)
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    val rowLayoutCategories = remember {
      PlaceCategory.entries.filter(PlaceCategory::showInRowLayout)
    }
    AnimatedContent(targetState = layout) {
      when (it) {
        PlacesLayout.Row -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            rowLayoutCategories.forEach { category ->
              PlaceCategoryItem(
                category = category,
                icon = category.icon,
                enabled = activeLocation != null,
                selected = category in places,
                state = places[category],
                layout = it,
                modifier = Modifier.weight(1f),
                alpha = alpha,
                animatedVisibilityScope = animatedVisibilityScope,
                onClick = { onTogglePlaceCategory(category) },
                onCategoryClick = onCategoryClick,
              )
            }
          }
        }
        PlacesLayout.Grid -> {
          LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            items(items = PlaceCategory.entries, key = PlaceCategory::name) { category ->
              PlaceCategoryItem(
                category = category,
                icon = category.icon,
                enabled = activeLocation != null,
                selected = category in places,
                state = places[category],
                layout = it,
                alpha = alpha,
                animatedVisibilityScope = animatedVisibilityScope,
                onClick = { onTogglePlaceCategory(category) },
                onCategoryClick = onCategoryClick,
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = stringResource(R.string.openstreetmap_attribution),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha / 2f),
      textAlign = TextAlign.Center,
    )
  }
}

private val PlaceCategory.showInRowLayout: Boolean
  get() =
    when (this) {
      PlaceCategory.SightsLandmarks,
      PlaceCategory.FoodDrink,
      PlaceCategory.Accommodation,
      PlaceCategory.Shopping -> true
      else -> false
    }
