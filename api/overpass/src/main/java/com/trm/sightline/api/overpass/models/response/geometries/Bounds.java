package com.trm.sightline.api.overpass.models.response.geometries;

public class Bounds {
  public final double minlat;
  public final double minlon;
  public final double maxlat;
  public final double maxlon;

  Bounds(double minlat, double minlon, double maxlat, double maxlon) {
    this.minlat = minlat;
    this.minlon = minlon;
    this.maxlat = maxlat;
    this.maxlon = maxlon;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (other == null) return false;
    if (getClass() != other.getClass()) return false;
    Bounds bounds = (Bounds) other;
    return Double.doubleToLongBits(bounds.minlat) == Double.doubleToLongBits(minlat)
        && Double.doubleToLongBits(bounds.minlon) == Double.doubleToLongBits(minlon)
        && Double.doubleToLongBits(bounds.maxlat) == Double.doubleToLongBits(maxlat)
        && Double.doubleToLongBits(bounds.maxlon) == Double.doubleToLongBits(maxlon);
  }

  @Override
  public int hashCode() {
    int tmp = Double.hashCode(minlat);
    tmp = 31 * tmp + Double.hashCode(minlon);
    tmp = 31 * tmp + Double.hashCode(maxlat);
    return 31 * tmp + Double.hashCode(maxlon);
  }

  @Override
  public String toString() {
    return String.format(
        "Bounds{minlat=%s, minlon=%s, maxlat=%s, maxlon=%s}", minlat, minlon, maxlat, maxlon);
  }
}
