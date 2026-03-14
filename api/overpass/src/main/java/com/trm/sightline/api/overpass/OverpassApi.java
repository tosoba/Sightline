package com.trm.sightline.api.overpass;

import com.squareup.moshi.Moshi;
import com.trm.sightline.api.overpass.models.response.OverpassResponse;
import com.trm.sightline.api.overpass.models.response.adapters.ElementAdapter;
import com.trm.sightline.api.overpass.models.response.adapters.Iso8601Adapter;
import com.trm.sightline.api.overpass.models.response.adapters.MemberAdapter;

import java.util.Date;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OverpassApi {

  String BASE_URL = "https://overpass-api.de";

  @GET("/api/interpreter")
  Call<OverpassResponse> ask(@Query("data") String data);

  static OverpassApi create() {
    return new Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(
            MoshiConverterFactory.create(
                new Moshi.Builder()
                    .add(new MemberAdapter())
                    .add(new ElementAdapter())
                    .add(Date.class, new Iso8601Adapter())
                    .build()))
        .build()
        .create(OverpassApi.class);
  }
}
