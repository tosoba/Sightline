package com.trm.sightline.api.overpass.models.response.geometries;

import com.trm.sightline.api.overpass.models.query.statements.base.Type;
import com.trm.sightline.api.overpass.models.response.geometries.members.Member;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

public final class Relation extends Geometry2D {
  public final Member[] members;

  Relation(
      Map<String, String> tags,
      long id,
      Date timestamp,
      int version,
      long changeset,
      String user,
      long uid,
      Bounds bounds,
      Coordinate center,
      Member[] members) {
    super(Type.RELATION, tags, id, timestamp, version, changeset, user, uid, bounds, center);
    this.members = members;
  }

  @Override
  public String toString() {
    return String.format(
        "Relation{type=%s, tags=%s, id=%s, timestamp=%s, version=%s, changeset=%s, "
            + "user=%s, uid=%s, bounds=%s, center=%s, members=%s}",
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
        Arrays.toString(members));
  }
}
