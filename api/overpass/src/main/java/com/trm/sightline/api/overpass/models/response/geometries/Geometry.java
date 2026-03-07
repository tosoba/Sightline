package com.trm.sightline.api.overpass.models.response.geometries;

import java.util.Date;
import java.util.Map;

public class Geometry extends Element {
  public final long id;
  public final Date timestamp;
  public final int version;
  public final long changeset;
  public final String user;
  public final long uid;

  Geometry(
      String type,
      Map<String, String> tags,
      long id,
      Date timestamp,
      int version,
      long changeset,
      String user,
      long uid) {
    super(type, tags);
    this.id = id;
    this.timestamp = timestamp;
    this.version = version;
    this.changeset = changeset;
    this.user = user;
    this.uid = uid;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (other == null) return false;
    if (getClass() != other.getClass()) return false;
    Geometry geometry = (Geometry) other;
    return geometry.id == id && geometry.type.equals(type);
  }

  @Override
  public int hashCode() {
    return 31 * type.hashCode() + Long.hashCode(id);
  }

  @Override
  public String toString() {
    return String.format(
        "Geometry{type=%s, tags=%s, id=%s, timestamp=%s, "
            + "version=%s, changeset=%s, user=%s, uid=%s}",
        type, tags, id, timestamp, version, changeset, user, uid);
  }
}
