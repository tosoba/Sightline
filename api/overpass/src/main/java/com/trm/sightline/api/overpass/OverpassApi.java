package com.trm.sightline.api.overpass;

import com.trm.sightline.api.overpass.models.response.OverpassResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * API to query OverpassApi-Language. Example query:
 * http://overpass-api.de/api/interpreter?data=[out:json];node(around:1600,52.516667,13.383333)["amenity"="post_box"];out
 * qt 13;
 */
public interface OverpassApi {

  String BASE_URL = "http://overpass-api.de";

  /**
   * Returns a OverpassApi response for the given query.
   *
   * @param data OverpassApi QL string data part Example:
   *     [out:json];node(around:1600,52.516667,13.383333)["amenity"="post_box"];out qt 13;
   * @return a call to execute the actual query.
   */
  @GET("/api/interpreter")
  Call<OverpassResponse> ask(@Query("data") String data);
}
