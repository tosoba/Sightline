package com.trm.sightline.core.common.location

sealed interface LocationSettingsState {
  data object Enabled : LocationSettingsState
  data object Disabled : LocationSettingsState
}
