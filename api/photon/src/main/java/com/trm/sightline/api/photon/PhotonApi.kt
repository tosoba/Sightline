package com.trm.sightline.api.photon

import com.mapbox.geojson.FeatureCollection
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import java.lang.reflect.Type

interface PhotonApi {
  @GET("api")
  suspend fun search(
    @Query("q") query: String,
    @Query("lat") lat: Double? = null,
    @Query("lon") lon: Double? = null,
    @Query("limit") limit: Int? = null,
    @Query("lang") lang: String? = null,
  ): FeatureCollection

  @GET("reverse")
  suspend fun reverse(
    @Query("lat") lat: Double,
    @Query("lon") lon: Double,
    @Query("limit") limit: Int? = null,
    @Query("lang") lang: String? = null,
  ): FeatureCollection

  companion object {
    private const val BASE_URL = "https://photon.komoot.io/"

    internal fun create(): PhotonApi =
      Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(OkHttpClient.Builder().build())
        .addConverterFactory(
          object : Converter.Factory() {
            override fun responseBodyConverter(
              type: Type,
              annotations: Array<out Annotation>,
              retrofit: Retrofit,
            ): Converter<ResponseBody, *>? {
              if (type == FeatureCollection::class.java) {
                return Converter<ResponseBody, FeatureCollection> { value ->
                  FeatureCollection.fromJson(value.string())
                }
              }
              return null
            }
          }
        )
        .build()
        .create(PhotonApi::class.java)
  }
}
