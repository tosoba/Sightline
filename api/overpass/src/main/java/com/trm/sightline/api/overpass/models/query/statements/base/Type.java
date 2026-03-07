package com.trm.sightline.api.overpass.models.query.statements.base;

/** Possible request types and the relation response tag. */
public interface Type {
  String NODE = "node";
  String WAY = "way";
  String RELATION = "rel";
  String RELATION_RESPONSE = "relation";
  String COUNT = "count";
  String AREA = "area";
}
