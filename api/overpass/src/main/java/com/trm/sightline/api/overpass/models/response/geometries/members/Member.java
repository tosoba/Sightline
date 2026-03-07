package com.trm.sightline.api.overpass.models.response.geometries.members;

import com.trm.sightline.api.overpass.models.response.geometries.Coordinate;

import java.util.List;

public class Member {
  public final String type;
  public final long ref;
  public final String role;
  public final double lat;
  public final double lon;
  public final List<Coordinate> geometry;

  Member(String type, long ref, String role, double lat, double lon, List<Coordinate> geometry) {
    this.type = type;
    this.ref = ref;
    this.role = role;
    this.lat = lat;
    this.lon = lon;
    this.geometry = geometry;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (other == null) return false;
    if (getClass() != other.getClass()) return false;
    Member member = (Member) other;
    return member.ref == ref;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(ref);
  }

  @Override
  public String toString() {
    return String.format("Member{type=%s, role=%s, ref=%s}", type, role, ref);
  }
}
