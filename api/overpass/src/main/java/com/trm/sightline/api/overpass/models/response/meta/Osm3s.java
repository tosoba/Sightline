package com.trm.sightline.api.overpass.models.response.meta;

public final class Osm3s {
  public final String timestamp_osm_base;
  public final String copyright;

  Osm3s(String timestamp_osm_base, String copyright) {
    this.timestamp_osm_base = timestamp_osm_base;
    this.copyright = copyright;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (other == null) return false;
    if (getClass() != other.getClass()) return false;
    Osm3s osm3s = (Osm3s) other;
    return osm3s.timestamp_osm_base.equals(timestamp_osm_base) && osm3s.copyright.equals(copyright);
  }

  @Override
  public int hashCode() {
    return 31 * timestamp_osm_base.hashCode() + copyright.hashCode();
  }

  @Override
  public String toString() {
    return String.format(
        "Osm3s{timestamp_osm_base=%s, copyright=%s}", timestamp_osm_base, copyright);
  }
}
