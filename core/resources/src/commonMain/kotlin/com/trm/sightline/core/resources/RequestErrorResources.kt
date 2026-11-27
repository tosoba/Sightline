package com.trm.sightline.core.resources

import com.trm.sightline.core.common.RequestError
import dev.icerock.moko.resources.StringResource

val RequestError.message: StringResource
  get() =
    when (this) {
      RequestError.IO -> MR.strings.error_connection
      is RequestError.Http -> MR.strings.error_server
      is RequestError.Other -> MR.strings.error_unknown
    }
