package com.trm.sightline.feature.places

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Atm
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.LocalPostOffice
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.MarkunreadMailbox
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.trm.sightline.core.model.CustomLocation
import com.trm.sightline.core.model.LoadingState
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.PlacesContent(
  places: Map<PlaceCategory, LoadingState<List<Place>>>,
  locationAddress: LoadingState<String>,
  userLocationEnabled: Boolean,
  placeCategoriesEnabled: Boolean,
  customLocationSearchResults: List<CustomLocation>,
  layout: PlacesLayout,
  alpha: Float,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier = Modifier,
  onSearchFocusChange: (Boolean) -> Unit,
  onToggleUserLocationEnabled: (Boolean) -> Unit,
  onCustomLocationAddressChange: (String) -> Unit,
  onCustomLocationSearchResultClick: (CustomLocation) -> Unit,
  onTogglePlaceCategory: (PlaceCategory) -> Unit,
  onCategoryClick: (PlaceCategory, List<Place>) -> Unit,
) {
  val focusManager = LocalFocusManager.current
  var isFocused by remember { mutableStateOf(false) }

  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    val expanded = isFocused && !userLocationEnabled && customLocationSearchResults.isNotEmpty()
    SearchBar(
      expanded = expanded,
      onExpandedChange = {},
      modifier = Modifier.fillMaxWidth(),
      colors =
        SearchBarDefaults.colors().run {
          copy(containerColor = containerColor.copy(alpha = alpha))
        },
      windowInsets = WindowInsets(),
      inputField = {
        SearchBarDefaults.InputField(
          query = if (locationAddress is LoadingState.Loaded) locationAddress.data else "",
          onQueryChange = onCustomLocationAddressChange,
          onSearch = {},
          expanded = expanded,
          enabled = !userLocationEnabled,
          onExpandedChange = {},
          placeholder = {
            Text(
              when {
                !userLocationEnabled -> "Enter your location."
                !placeCategoriesEnabled -> "Loading location..."
                locationAddress is LoadingState.Loading -> "Loading location address..."
                else -> "Location address not found."
              }
            )
          },
          modifier =
            Modifier.onFocusChanged {
              isFocused = it.isFocused
              onSearchFocusChange(it.isFocused)
            },
          leadingIcon = {
            when {
              locationAddress is LoadingState.Loading ||
                (userLocationEnabled && !placeCategoriesEnabled) -> {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
              }
              userLocationEnabled && locationAddress is LoadingState.Loaded -> {
                Icon(imageVector = Icons.Default.GpsFixed, contentDescription = null)
              }
              isFocused -> {
                IconButton(onClick = focusManager::clearFocus) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                  )
                }
              }
              else -> {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
              }
            }
          },
          trailingIcon = {
            FilledTonalIconToggleButton(
              checked = userLocationEnabled,
              onCheckedChange = onToggleUserLocationEnabled,
            ) {
              Icon(imageVector = Icons.Default.MyLocation, contentDescription = "My location")
            }
          },
        )
      },
    ) {
      LazyColumn {
        if (locationAddress is LoadingState.Loaded && locationAddress.data.trim().length < 3) {
          item {
            Box(
              modifier = Modifier.fillParentMaxWidth().padding(16.dp).animateItem(),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text =
                  if (locationAddress.data.trim().isEmpty()) {
                    "Start typing to search for a location."
                  } else {
                    "Your query must be at least 3 characters long."
                  },
                style = MaterialTheme.typography.titleLarge,
              )
            }
          }
        }

        items(customLocationSearchResults) { result ->
          ListItem(
            headlineContent = { Text(result.address) },
            leadingContent = { Icon(imageVector = Icons.Default.Place, contentDescription = null) },
            colors =
              ListItemDefaults.colors(
                containerColor = ListItemDefaults.colors().containerColor.copy(alpha = 0f)
              ),
            modifier =
              Modifier.animateItem().clickable {
                focusManager.clearFocus()
                onCustomLocationSearchResultClick(result)
              },
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    AnimatedContent(targetState = layout) {
      when (it) {
        PlacesLayout.Row -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            PlaceCategory.entries.filter(PlaceCategory::showInRowLayout).forEach { category ->
              PlaceCategoryItem(
                category = category,
                icon = category.icon,
                enabled = placeCategoriesEnabled,
                selected = category in places,
                loadingState = places[category],
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
            items(PlaceCategory.entries) { category ->
              PlaceCategoryItem(
                category = category,
                icon = category.icon,
                enabled = placeCategoriesEnabled,
                selected = category in places,
                loadingState = places[category],
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
  }
}

private val PlaceCategory.showInRowLayout: Boolean
  get() =
    when (this) {
      PlaceCategory.Attractions,
      PlaceCategory.Food,
      PlaceCategory.Accommodation,
      PlaceCategory.Stores -> true
      else -> false
    }

private val PlaceCategory.icon: ImageVector
  get() =
    when (this) {
      PlaceCategory.Attractions -> Icons.Default.Place
      PlaceCategory.Food -> Icons.Default.Restaurant
      PlaceCategory.Accommodation -> Icons.Default.Hotel
      PlaceCategory.Stores -> Icons.Default.Storefront
      PlaceCategory.BikeRental -> Icons.AutoMirrored.Filled.DirectionsBike
      PlaceCategory.BusStation -> Icons.Default.DirectionsBus
      PlaceCategory.CarRental -> Icons.Default.DirectionsCar
      PlaceCategory.CarWash -> Icons.Default.LocalCarWash
      PlaceCategory.ChargingStation -> Icons.Default.EvStation
      PlaceCategory.Fuel -> Icons.Default.LocalGasStation
      PlaceCategory.Parking -> Icons.Default.LocalParking
      PlaceCategory.Taxi -> Icons.Default.LocalTaxi
      PlaceCategory.Atm -> Icons.Default.Atm
      PlaceCategory.Bank -> Icons.Default.AccountBalance
      PlaceCategory.CurrencyExchange -> Icons.Default.CurrencyExchange
      PlaceCategory.Doctors -> Icons.Default.MedicalServices
      PlaceCategory.Hospital -> Icons.Default.LocalHospital
      PlaceCategory.Pharmacy -> Icons.Default.LocalPharmacy
      PlaceCategory.Veterinary -> Icons.Default.Pets
      PlaceCategory.Casino -> Icons.Default.Casino
      PlaceCategory.Cinema -> Icons.Default.Movie
      PlaceCategory.CommunityCentre -> Icons.Default.Groups
      PlaceCategory.Library -> Icons.Default.LocalLibrary
      PlaceCategory.Nightclub -> Icons.Default.Nightlife
      PlaceCategory.Theatre -> Icons.Default.TheaterComedy
      PlaceCategory.FireStation -> Icons.Default.LocalFireDepartment
      PlaceCategory.ParcelLocker -> Icons.Default.Inventory
      PlaceCategory.Police -> Icons.Default.LocalPolice
      PlaceCategory.PostBox -> Icons.Default.MarkunreadMailbox
      PlaceCategory.PostOffice -> Icons.Default.LocalPostOffice
      PlaceCategory.Toilets -> Icons.Default.Wc
    }
