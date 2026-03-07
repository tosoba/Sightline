package com.trm.sightline.api.overpass.models.response.geometries;

import java.util.Date;
import java.util.Map;

public class Geometry2D extends Geometry {
  public final Bounds bounds;
  public final Coordinate center;

  Geometry2D(
      String type,
      Map<String, String> tags,
      long id,
      Date timestamp,
      int version,
      long changeset,
      String user,
      long uid,
      Bounds bounds,
      Coordinate center) {
    super(type, tags, id, timestamp, version, changeset, user, uid);
    this.bounds = bounds;
    this.center = center;
  }

  @Override
  public String toString() {
    return String.format(
        "Geometry2D{type=%s, tags=%s, id=%s, timestamp=%s, "
            + "version=%s, changeset=%s, user=%s, uid=%s, bounds=%s, center=%s}",
        type, tags, id, timestamp, version, changeset, user, uid, bounds, center);
  }
}
