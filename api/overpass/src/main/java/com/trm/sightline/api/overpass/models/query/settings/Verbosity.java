package com.trm.sightline.api.overpass.models.query.settings;

/**
 * Degree of verbosity, overpass uses body as default.
 *
 * <p>IDS: Print only the ids of the elements. SKELETON: Print also the information necessary for
 * geometry. These are also coordinates for nodes and way and relation member ids for ways and
 * relations. BODY: Print all information necessary to use the data. These are also tags for all
 * elements and the roles for relation members. TAGS: Print only ids and tags for each element and
 * not coordinates or members. META: Print everything known about the elements. This includes
 * additionally to body for all elements the version, changeset id, timestamp and the user data of
 * the user that last touched the object.
 */
public enum Verbosity {
  BODY("body"),
  SKELETON("skel"),
  META("meta"),
  IDS("ids"),
  TAGS("tags");

  public final String sign;

  Verbosity(String sign) {
    this.sign = sign;
  }
}
