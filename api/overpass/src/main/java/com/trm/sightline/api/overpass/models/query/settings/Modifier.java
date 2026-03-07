package com.trm.sightline.api.overpass.models.query.settings;

/**
 * Modifiers for derived information.
 *
 * <p>BOUNDING_BOX: Adds the bounding box of each element to the element. For nodes this is
 * equivalent to "geom". For ways it is the enclosing bounding box of all nodes. For relations it is
 * the enclosing bounding box of all node and way members, relations as members have no effect.
 * CENTER: This adds the center of the above mentioned bounding box to ways and relations. Note: The
 * center point is not guaranteed to lie inside the polygon. GEOM: Add the full geometry to each
 * object. This adds coordinates to each node, to each node member of a way or relation, and it adds
 * a sequence of "nd" members with coordinates to all relations. COUNT Get the element count.
 */
public enum Modifier {
  BOUNDING_BOX("bb"),
  CENTER("center"),
  GEOM("geom"),
  COUNT("count");

  public final String sign;

  Modifier(String sign) {
    this.sign = sign;
  }
}
