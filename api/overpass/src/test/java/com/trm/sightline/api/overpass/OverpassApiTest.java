package com.trm.sightline.api.overpass;

import com.trm.sightline.api.overpass.models.query.settings.Filter;
import com.trm.sightline.api.overpass.models.query.statements.NodeQuery;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class OverpassApiTest {
  private OverpassApi overpassApi;

  @Before
  public void setUp() {
    overpassApi =
        new Retrofit.Builder()
            .baseUrl(OverpassApi.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(OverpassApi.class);
  }

  @Test
  public void singleAmenity() throws IOException {
    System.out.println(
        overpassApi
            .ask(
                new NodeQuery.Builder()
                    .timeout(25)
                    .tag("amenity", "post_box")
                    .around(52.5, 13.4, 500.0f)
                    .build()
                    .toQuery())
            .execute()
            .body()
            .toString());
  }

  @Test
  public void allAmenities() throws IOException {
    System.out.println(
        overpassApi
            .ask(
                new NodeQuery.Builder()
                    .timeout(25)
                    .is("amenity")
                    .around(52.5, 13.4, 500.0f)
                    .build()
                    .toQuery())
            .execute()
            .body()
            .toString());
  }

  @Test
  public void multipleAmenities() throws IOException {
    final String query =
        new NodeQuery.Builder()
            .timeout(25)
            .tag(
                "amenity",
                "bar|biergarten|cafe|fast_food|food_court|ice_cream|pub|restaurant",
                Filter.LIKE)
            .around(52.5, 13.4, 500.0f)
            .build()
            .toQuery();
    System.out.println(query);
    System.out.println(overpassApi.ask(query).execute().body().toString());
  }
}
