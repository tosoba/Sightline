package com.trm.sightline.api.nominatim

import com.trm.sightline.api.nominatim.models.responses.DetailsResponse
import com.trm.sightline.api.nominatim.models.responses.OSMGeocodeJson
import com.trm.sightline.api.nominatim.models.responses.StatusResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApi {
  @GET("search")
  suspend fun search(
    @Query("q") query: String? = null,
    @Query("amenity") amenity: String? = null,
    @Query("street") street: String? = null,
    @Query("city") city: String? = null,
    @Query("county") county: String? = null,
    @Query("state") state: String? = null,
    @Query("country") country: String? = null,
    @Query("postalcode") postalcode: String? = null,
    @Query("format") format: String = "geocodejson",
    @Query("json_callback") jsonCallback: String? = null,
    @Query("limit") limit: Int? = null,
    @Query("addressdetails") addressdetails: Int? = null,
    @Query("extratags") extratags: Int? = null,
    @Query("namedetails") namedetails: Int? = null,
    @Query("accept-language") acceptLanguage: String? = null,
    @Query("countrycodes") countrycodes: String? = null,
    @Query("layer") layer: String? = null,
    @Query("featureType") featureType: String? = null,
    @Query("exclude_place_ids") excludePlaceIds: String? = null,
    @Query("viewbox") viewbox: String? = null,
    @Query("bounded") bounded: Int? = null,
    @Query("polygon_geojson") polygonGeojson: Int? = null,
    @Query("polygon_kml") polygonKml: Int? = null,
    @Query("polygon_svg") polygonSvg: Int? = null,
    @Query("polygon_text") polygonText: Int? = null,
    @Query("polygon_threshold") polygonThreshold: Double? = null,
    @Query("email") email: String? = null,
    @Query("dedupe") dedupe: Int? = null,
    @Query("debug") debug: Int? = null,
  ): OSMGeocodeJson

  @GET("reverse")
  suspend fun reverse(
    @Query("lat") lat: Double,
    @Query("lon") lon: Double,
    @Query("format") format: String = "geocodejson",
    @Query("json_callback") jsonCallback: String? = null,
    @Query("addressdetails") addressdetails: Int? = null,
    @Query("extratags") extratags: Int? = null,
    @Query("namedetails") namedetails: Int? = null,
    @Query("accept-language") acceptLanguage: String? = null,
    @Query("zoom") zoom: Int? = null,
    @Query("layer") layer: String? = null,
    @Query("polygon_geojson") polygonGeojson: Int? = null,
    @Query("polygon_kml") polygonKml: Int? = null,
    @Query("polygon_svg") polygonSvg: Int? = null,
    @Query("polygon_text") polygonText: Int? = null,
    @Query("polygon_threshold") polygonThreshold: Double? = null,
    @Query("email") email: String? = null,
    @Query("debug") debug: Int? = null,
  ): OSMGeocodeJson

  @GET("lookup")
  suspend fun lookup(
    @Query("osm_ids") osmIds: String,
    @Query("format") format: String = "geocodejson",
    @Query("json_callback") jsonCallback: String? = null,
    @Query("addressdetails") addressdetails: Int? = null,
    @Query("extratags") extratags: Int? = null,
    @Query("namedetails") namedetails: Int? = null,
    @Query("accept-language") acceptLanguage: String? = null,
    @Query("polygon_geojson") polygonGeojson: Int? = null,
    @Query("polygon_kml") polygonKml: Int? = null,
    @Query("polygon_svg") polygonSvg: Int? = null,
    @Query("polygon_text") polygonText: Int? = null,
    @Query("polygon_threshold") polygonThreshold: Double? = null,
    @Query("email") email: String? = null,
    @Query("debug") debug: Int? = null,
  ): List<OSMGeocodeJson>

  @GET("status") suspend fun status(@Query("format") format: String = "json"): StatusResponse

  @GET("details")
  suspend fun details(
    @Query("osmtype") osmtype: String? = null,
    @Query("osmid") osmid: Long? = null,
    @Query("class") className: String? = null,
    @Query("place_id") placeId: Int? = null,
    @Query("format") format: String = "json",
    @Query("json_callback") jsonCallback: String? = null,
    @Query("addressdetails") addressdetails: Int? = null,
    @Query("keywords") keywords: Int? = null,
    @Query("linkedplaces") linkedplaces: Int? = null,
    @Query("hierarchy") hierarchy: Int? = null,
    @Query("group_hierarchy") groupHierarchy: Int? = null,
    @Query("polygon_geojson") polygonGeojson: Int? = null,
    @Query("accept-language") acceptLanguage: String? = null,
  ): DetailsResponse

  companion object {
    private const val BASE_URL = "https://nominatim.openstreetmap.org/"

    fun create(): NominatimApi =
      Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(
          OkHttpClient.Builder()
            .addInterceptor { chain ->
              chain.proceed(
                chain
                  .request()
                  .newBuilder()
                  .header("User-Agent", BuildConfig.NOMINATIM_USER_AGENT)
                  .build()
              )
            }
            .build()
        )
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(NominatimApi::class.java)
  }
}
