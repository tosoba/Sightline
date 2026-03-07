package com.trm.sightline.api.overpass.models.query.settings;

import com.trm.sightline.api.overpass.models.query.Convertible;

/**
 * Settings determine the OverpassApi output format, the maximum size of the response in bytes, the
 * a timeout limit of the query and optionally a global bounding box. The default header has a
 * maximum response size of 10MB and a timeout limit of 10 seconds.
 */
public class Settings implements Convertible<Settings.Builder> {

  private static final String OUTPUT_FORMAT = "[out:json]";
  private static final long MAXSIZE_LIMIT = 1073741824;
  private static final long MAXSIZE_DEFAULT = 10485760; // ~ 10 MB
  private static final int TIMEOUT_DEFAULT = 10; // 10 seconds

  private final double[] coordinates;
  private final int timeout;
  private final long maxsize;

  public static Settings getDefault() {
    return new Settings.Builder().build();
  }

  private Settings(Builder builder) {
    this.coordinates = builder.coordinates;
    this.timeout = builder.timeout;
    this.maxsize = builder.maxsize;
  }

  /**
   * Gets {@link Settings.Builder} from {@link Settings}.
   *
   * @return String
   */
  @Override
  public Settings.Builder toBuilder() {
    return new Settings.Builder().timeout(timeout).size(maxsize).globalBoundingBox(coordinates);
  }

  /**
   * Get {@link Settings} header as {@link String}.
   *
   * @return {@link String}
   */
  @Override
  public String toQuery() {
    String boundingBox = coordinates == null ? "" : boundingBox();
    return String.format(
        "%s[timeout:%s][maxsize:%s]%s", OUTPUT_FORMAT, timeout, maxsize, boundingBox);
  }

  private String boundingBox() {
    return String.format(
        "[bbox:%s,%s,%s,%s]", coordinates[0], coordinates[1], coordinates[2], coordinates[3]);
  }

  public static final class Builder {

    private double[] coordinates = null;
    private int timeout = TIMEOUT_DEFAULT;
    private long maxsize = MAXSIZE_DEFAULT;

    /**
     * Set OverpassApi query timeout limit.
     *
     * @param timeout integer seconds until
     * @return {@link Builder}
     */
    public Builder timeout(int timeout) {
      this.timeout = timeout;
      validateParams();
      return this;
    }

    /**
     * Set OverpassApi' maximum response size in bytes.
     *
     * @param maxsize maximum size in bytes as long.
     * @return {@link Builder}
     */
    public Builder size(long maxsize) {
      this.maxsize = maxsize;
      validateParams();
      return this;
    }

    /**
     * Set global BoundingBox for query.
     *
     * <p>e.g. [boundingBox:south,west,north,east]
     *
     * @param coordinates in the format of [south, west, north, east] in degrees.
     * @return {@link Builder}
     */
    public Builder globalBoundingBox(double[] coordinates) {
      if (coordinates == null) return this;
      validateBBoxParams(coordinates);
      this.coordinates = coordinates;
      return this;
    }

    /**
     * Builds {@link Settings}.
     *
     * @return {@link Settings}
     */
    public Settings build() {
      return new Settings(this);
    }

    private void validateParams() {
      if (timeout < 1) {
        throw new IllegalArgumentException(
            "Expected timeout to be more than 1 second, but was " + timeout);
      }
      if (maxsize < 1) {
        throw new IllegalArgumentException(
            "Expected maximum size to be more than 1 byte, but was " + maxsize);
      }
      if (maxsize > MAXSIZE_LIMIT) {
        throw new IllegalArgumentException("Maximum size must be smaller than " + MAXSIZE_LIMIT);
      }
    }

    private void validateBBoxParams(double[] coordinates) {
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

    private void validateLatLng(double lat, double lng) {
      if (lat < -90 || lat > 90) {
        throw new IllegalArgumentException("Expected latitude between -90 and 90, but was " + lat);
      }
      if (lng < -180 || lng > 180) {
        throw new IllegalArgumentException(
            "Expected longitude between -180 and 180, but was " + lng);
      }
    }
  }
}
