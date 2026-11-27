package com.trm.sightline.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.icerock.moko.resources.StringResource

@Composable
fun StringResource.localized(): String = stringResource(resourceId)

@Composable
fun StringResource.localized(vararg args: Any): String = stringResource(resourceId, *args)
