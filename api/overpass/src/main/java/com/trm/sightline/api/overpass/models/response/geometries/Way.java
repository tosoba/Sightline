package com.trm.sightline.api.overpass.models.response.geometries;

import com.trm.sightline.api.overpass.models.query.statements.base.Type;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

public final class Way extends Geometry2D {
  public final long[] nodes;
  public final Coordinate[] geometry;

  Way(
      Map<String, String> tags,
      long id,
      Date timestamp,
      int version,
      long changeset,
      String user,
      long uid,
      Bounds bounds,
      Coordinate center,
      long[] nodes,
      Coordinate[] geometry) {
    super(Type.WAY, tags, id, timestamp, version, changeset, user, uid, bounds, center);
    this.nodes = nodes;
    this.geometry = geometry;
  }

  @Override
  public String toString() {
    return String.format(
        "Way{type=%s, tags=%s, id=%s, timestamp=%s, version=%s, changeset=%s, "
            + "user=%s, uid=%s, bounds=%s, center=%s, nodes=%s, geometry=%s}",
        type,
        tags,
        id,
        timestamp,
        version,
        changeset,
        user,
        uid,
        bounds,
        center,
        Arrays.toString(nodes),
        Arrays.toString(geometry));
  }
}
