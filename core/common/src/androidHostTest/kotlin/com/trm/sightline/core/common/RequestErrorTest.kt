package com.trm.sightline.core.common

import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RequestErrorTest {
  @Test
  fun `maps an io exception to the connection error`() {
    assertEquals(RequestError.IO, IOException("offline").toRequestError())
  }

  @Test
  fun `keeps unexpected exceptions as other`() {
    assertIs<RequestError.Other>(IllegalStateException("boom").toRequestError())
  }
}
