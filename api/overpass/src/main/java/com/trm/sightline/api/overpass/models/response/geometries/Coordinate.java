package com.trm.sightline.api.overpass.models.response.geometries;

public class Coordinate {
  public final double lat;
  public final double lon;

  public Coordinate(double lat, double lon) {
    this.lat = lat;
    this.lon = lon;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (other == null) return false;
    if (getClass() != other.getClass()) return false;
    Coordinate coordinate = (Coordinate) other;
    return Double.doubleToLongBits(coordinate.lat) == Double.doubleToLongBits(lat)
        && Double.doubleToLongBits(coordinate.lon) == Double.doubleToLongBits(lon);
  }

  @Override
  public int hashCode() {
    return 31 * Double.hashCode(lat) + Double.hashCode(lon);
  }

  @Override
  public String toString() {
    return String.format("Coordinate{lat=%s, lon=%s}", lat, lon);
  }
}
