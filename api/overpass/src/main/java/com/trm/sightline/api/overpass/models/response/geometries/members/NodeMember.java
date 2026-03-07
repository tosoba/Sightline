package com.trm.sightline.api.overpass.models.response.geometries.members;

public class NodeMember extends Member {

  NodeMember(String type, long ref, String role, double lat, double lon) {
    super(type, ref, role, lat, lon, null);
  }

  @Override
  public String toString() {
    return String.format(
        "NodeMember{type=%s, role=%s, ref=%s, lat=%s, lon=%s}", type, role, ref, lat, lon);
  }
}
