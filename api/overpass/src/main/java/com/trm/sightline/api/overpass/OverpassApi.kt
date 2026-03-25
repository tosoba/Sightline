package com.trm.sightline.api.overpass

import com.squareup.moshi.Moshi
import com.trm.sightline.api.overpass.models.response.OverpassResponse
import com.trm.sightline.api.overpass.models.response.adapters.ElementAdapter
import com.trm.sightline.api.overpass.models.response.adapters.Iso8601Adapter
import com.trm.sightline.api.overpass.models.response.adapters.MemberAdapter
import java.util.Date
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface OverpassApi {
  @GET("/api/interpreter") suspend fun ask(@Query("data") data: String): OverpassResponse

  companion object {
    private const val BASE_URL = "https://overpass-api.de"

    internal fun create(client: OkHttpClient = OkHttpClient()): OverpassApi =
      Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(
          MoshiConverterFactory.create(
            Moshi.Builder()
              .add(MemberAdapter())
              .add(ElementAdapter())
              .add(Date::class.java, Iso8601Adapter())
              .build()
          )
        )
        .build()
        .create(OverpassApi::class.java)
  }
}
