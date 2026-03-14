package com.trm.sightline.api.overpass;

import com.trm.sightline.api.overpass.models.query.settings.Filter;
import com.trm.sightline.api.overpass.models.query.statements.NodeQuery;
import java.io.IOException;
import org.junit.Test;

public class OverpassApiTest {
  private final OverpassApi overpassApi = OverpassApi.create();

  @Test
  public void singleAmenity() throws IOException {
    System.out.println(
        overpassApi
            .ask(
                new NodeQuery.Builder()
                    .timeout(25)
                    .tag("amenity", "post_box")
                    .around(52.237049, 21.017532, 1000f)
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
                    .around(52.237049, 21.017532, 1000f)
                    .build()
                    .toQuery())
            .execute()
            .body()
            .toString());
  }

  @Test
  public void foodAmenities() throws IOException {
    final String query =
        new NodeQuery.Builder()
            .timeout(25)
            .tag(
                "amenity",
                "bar|biergarten|cafe|fast_food|food_court|ice_cream|pub|restaurant",
                Filter.LIKE)
            .around(52.237049, 21.017532, 1000f)
            .build()
            .toQuery();
    System.out.println(query);
    System.out.println(overpassApi.ask(query).execute().body().toString());
  }

  @Test
  public void tourismAccommodation() throws IOException {
    final String query =
        new NodeQuery.Builder()
            .timeout(25)
            .tag(
                "tourism",
                "alpine_hut|apartment|chalet|guest_house|hostel|hotel|motel",
                Filter.LIKE)
            .around(52.237049, 21.017532, 1000f)
            .build()
            .toQuery();
    System.out.println(query);
    System.out.println(overpassApi.ask(query).execute().body().toString());
  }

  @Test
  public void tourismPlacesToSee() throws IOException {
    final String query =
        new NodeQuery.Builder()
            .timeout(25)
            .tag(
                "tourism",
                "artwork|attraction|aquarium|gallery|museum|theme_park|viewpoint|zoo",
                Filter.LIKE)
            .around(52.237049, 21.017532, 1000f)
            .build()
            .toQuery();
    System.out.println(query);
    System.out.println(overpassApi.ask(query).execute().body().toString());
  }

  @Test
  public void leisure() throws IOException {
    final String query =
        new NodeQuery.Builder()
            .timeout(25)
            .tag(
                "leisure",
                "adult_gaming_centre|amusement_arcade|beach_resort|bowling_alley|dance|disc_golf_course|dog_park|escape_game|fishing|fitness_centre|fitness_station|garden|golf_course|hackerspace|high_ropes_course|horse_riding|ice_rink|marina|miniature_gold|nature_reserve|park|pitch|playground|resort|sauna|sports_centre|sports_hall|stadium|sunbathing|swimming_pool|track|trampoline_park",
                Filter.LIKE)
            .around(52.237049, 21.017532, 1000f)
            .build()
            .toQuery();
    System.out.println(query);
    System.out.println(overpassApi.ask(query).execute().body().toString());
  }

  @Test
  public void generalStores() throws IOException {
    final String query =
        new NodeQuery.Builder()
            .timeout(25)
            .tag("shop", "department_store|general|kiosk|mall|supermarket|wholesale", Filter.LIKE)
            .around(52.237049, 21.017532, 1000f)
            .build()
            .toQuery();
    System.out.println(query);
    System.out.println(overpassApi.ask(query).execute().body().toString());
  }
}
