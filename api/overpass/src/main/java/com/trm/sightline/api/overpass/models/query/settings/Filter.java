package com.trm.sightline.api.overpass.models.query.settings;

/** Tag request clauses (or "tag filters") */
public enum Filter {
  EQUAL("\"=\""),
  NOT_EQUAL("\"!=\""),
  IS(""),
  IS_NOT("!"),
  LIKE("\"~\""),
  NOT_LIKE("\"!~\""),
  ILIKE("\"~\""),
  NOT_ILIKE("\"!~\"");

  public final String sign;

  Filter(String sign) {
    this.sign = sign;
  }
}
