package com.trm.sightline.api.overpass.models.response;

import com.trm.sightline.api.overpass.models.response.geometries.Element;
import com.trm.sightline.api.overpass.models.response.meta.Osm3s;

import java.util.Arrays;

public final class OverpassResponse {
  public final double version;
  public final String generator;
  public final Osm3s osm3s;
  public final Element[] elements;

  OverpassResponse(double version, String generator, Osm3s osm3s, Element[] elements) {
    this.version = version;
    this.generator = generator;
    this.osm3s = osm3s;
    this.elements = elements;
  }

  @Override
  public String toString() {
    return String.format(
        "OverpassResponse{version=%s, generator=%s, osm3s=%s, elements=%s}",
        version, generator, osm3s, Arrays.toString(elements));
  }
}
