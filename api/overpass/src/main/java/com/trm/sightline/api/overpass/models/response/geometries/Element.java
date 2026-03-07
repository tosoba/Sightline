package com.trm.sightline.api.overpass.models.response.geometries;

import java.util.Map;

public class Element {
  public final String type;
  public final Map<String, String> tags;

  Element(String type, Map<String, String> tags) {
    this.type = type;
    this.tags = tags;
  }

  @Override
  public String toString() {
    return String.format("Element{type=%s, tags=%s}", type, tags);
  }
}
