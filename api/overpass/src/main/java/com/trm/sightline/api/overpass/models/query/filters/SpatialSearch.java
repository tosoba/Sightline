package com.trm.sightline.api.overpass.models.query.filters;

import com.trm.sightline.api.overpass.models.query.settings.SpatialSearchType;
import com.trm.sightline.api.overpass.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a spatial search type. Possibilities are...
 *
 * <p>- Around (around a point coordinate within a specified radius) - Area (within an area defined
 * by a set or relation) - Bounding Box (within a defined square) - Polygon (withing a defined
 * polygon)
 */
public class SpatialSearch {

  private final SpatialSearchType type;
  private String area;
  private float radius;
  private double longitude;
  private double latitude;
  private double[] coordinates;

  /**
   * Constructor of type AROUND.
   *
   * @param type of search
   * @param latitude double
   * @param longitude double
   * @param radius of the search area in meters
   */
  public SpatialSearch(SpatialSearchType type, double latitude, double longitude, float radius) {
    this.type = type;
    this.radius = radius;
    this.latitude = latitude;
    this.longitude = longitude;
    validateAroundParams();
  }

  /**
   * Constructor of type AREA.
   *
   * @param type of search
   * @param area e.g. name of a city
   */
  public SpatialSearch(SpatialSearchType type, String area) {
    this.type = type;
    this.area = area;
    if (type != SpatialSearchType.AREA) {
      throw new IllegalArgumentException("wrong search type!");
    }
  }

  /**
   * Constructor of type BOUNDING_BOX.
   *
   * @param type of search
   * @param coordinates in the format of [south, west, north, east]
   */
  public SpatialSearch(SpatialSearchType type, double[] coordinates) {
    this.type = type;
    this.coordinates = coordinates;
    validateBBoxParams();
  }

  /**
   * Constructor of type POLYGON.
   *
   * @param type of search
   * @param coordinates in the format of [[lat1, lng1], [lat2, lng2], ...]
   */
  public SpatialSearch(SpatialSearchType type, double[][] coordinates) {
    this.type = type;
    validatePolyParams(coordinates);
  }

  /**
   * Get wrapped query part of specified search region type.
   *
   * @return String
   */
  public String create() {
    switch (type) {
      case BOUNDING_BOX:
        return String.format("(%s)", getFormattedCoordinates(","));
      case POLYGON:
        return String.format("(poly:\"%s\")", getFormattedCoordinates(" "));
      case AROUND:
        return String.format("(around:%s,%s,%s)", radius, latitude, longitude);
      case AREA:
        return String.format("area[\"name\"=\"%s\"]", area);
      default:
        return "";
    }
  }

  /**
   * Get specified SpatialSearchType.
   *
   * @return {@link SpatialSearchType}
   */
  public SpatialSearchType getType() {
    return type;
  }

  private String getFormattedCoordinates(String delimiter) {
    List<String> coo = new ArrayList<>();
    for (double coordinate : coordinates) {
      coo.add(String.valueOf(coordinate));
    }
    return StringUtils.join(delimiter, coo);
  }

  private void validateAroundParams() {
    if (type != SpatialSearchType.AROUND) {
      throw new IllegalArgumentException("wrong search type!");
    }
    validateLatLng(latitude, longitude);
    if (radius < 2) {
      throw new IllegalArgumentException(
          "Expected maximum radius to be greater then one, but was " + radius);
    }
  }

  private void validateBBoxParams() {
    if (type != SpatialSearchType.BOUNDING_BOX) {
      throw new IllegalArgumentException("wrong search type!");
    }
    if (coordinates.length != 4) {
      throw new IllegalArgumentException("Expected 4 coordinates, but got " + coordinates.length);
    }
    validateLatLng(coordinates[0], coordinates[1]);
    validateLatLng(coordinates[2], coordinates[3]);
    if (coordinates[0] > coordinates[2]) {
      throw new IllegalArgumentException(
          String.format(
              "Expected south (lat=%s) to be less than north (lat=%s)",
              coordinates[0], coordinates[2]));
    }
    if (coordinates[1] > coordinates[3]) {
      throw new IllegalArgumentException(
          String.format(
              "Expected west (long=%s) to be less than east (long=%s)",
              coordinates[1], coordinates[3]));
    }
  }

  private void validatePolyParams(double[][] coordinates) {
    if (type != SpatialSearchType.POLYGON) {
      throw new IllegalArgumentException("wrong search type!");
    }
    int counter = 0;
    this.coordinates = new double[2 * coordinates.length];
    if (coordinates.length < 3) {
      throw new IllegalArgumentException(
          "Expected at least 3 pairs of coordinates, but got " + coordinates.length);
    }
    for (double[] pair : coordinates) {
      if (pair.length != 2) {
        throw new IllegalArgumentException(
            "Expected 2 coordinates to build a node, but got " + pair.length);
      }
      validateLatLng(pair[0], pair[1]);
      this.coordinates[counter] = pair[0];
      this.coordinates[++counter] = pair[1];
      ++counter;
    }
  }

  private void validateLatLng(double lat, double lng) {
    if (lat < -90 || lat > 90) {
      throw new IllegalArgumentException("Expected latitude between -90 and 90, but was " + lat);
    }
    if (lng < -180 || lng > 180) {
      throw new IllegalArgumentException("Expected longitude between -180 and 180, but was " + lng);
    }
  }
}
