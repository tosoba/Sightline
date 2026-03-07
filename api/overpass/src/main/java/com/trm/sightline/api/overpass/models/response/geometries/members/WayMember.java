package com.trm.sightline.api.overpass.models.response.geometries.members;

import com.trm.sightline.api.overpass.models.response.geometries.Coordinate;

import java.util.List;

public class WayMember extends Member {

  public WayMember(String type, long ref, String role, List<Coordinate> geometry) {
    super(type, ref, role, 0, 0, geometry);
  }

  @Override
  public String toString() {
    return String.format(
        "WayMember{type=%s, role=%s, ref=%s, geometry=%s}", type, role, ref, geometry);
  }
}
