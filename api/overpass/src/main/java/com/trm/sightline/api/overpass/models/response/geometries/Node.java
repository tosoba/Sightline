package com.trm.sightline.api.overpass.models.response.geometries;

import com.trm.sightline.api.overpass.models.query.statements.base.Type;

import java.util.Date;
import java.util.Map;

public final class Node extends Geometry {
  public final double lat;
  public final double lon;

  Node(
      Map<String, String> tags,
      long id,
      Date timestamp,
      int version,
      long changeset,
      String user,
      long uid,
      double lat,
      double lon) {
    super(Type.NODE, tags, id, timestamp, version, changeset, user, uid);
    this.lat = lat;
    this.lon = lon;
  }

  @Override
  public String toString() {
    return String.format(
        "Node{type=%s, tags=%s, id=%s, timestamp=%s, "
            + "version=%s, changeset=%s, user=%s, uid=%s, lat=%s, lon=%s}",
        type, tags, id, timestamp, version, changeset, user, uid, lat, lon);
  }
}
