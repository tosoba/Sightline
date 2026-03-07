package com.trm.sightline.api.overpass.models.query.standalone;

import com.trm.sightline.api.overpass.models.query.statements.ComplexQuery;
import com.trm.sightline.api.overpass.models.query.statements.base.Statement;

/**
 * A {@link Recursion} defines the inclusion of lower or higher order {@link ComplexQuery.Geometry}
 * types.
 */
public enum Recursion implements Statement {
  DOWN(">"), // Recurse down standalone query
  DOUBLE_DOWN(">>"), // Recurse down relations query
  UP("<"), // recurse up standalone query
  DOUBLE_UP("<<"); // recurse up relations query

  private final String sign;

  Recursion(String sign) {
    this.sign = sign;
  }

  @Override
  public boolean isSuccessor() {
    return true;
  }

  @Override
  public String create() {
    return sign;
  }
}
