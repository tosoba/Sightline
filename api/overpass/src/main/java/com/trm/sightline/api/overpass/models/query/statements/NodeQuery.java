package com.trm.sightline.api.overpass.models.query.statements;

import com.trm.sightline.api.overpass.models.query.settings.Selection;
import com.trm.sightline.api.overpass.models.query.statements.base.GeoQuery;
import com.trm.sightline.api.overpass.models.query.statements.base.Type;

public final class NodeQuery extends GeoQuery {

  private NodeQuery(Builder builder) {
    super(builder);
  }

  public static class Builder extends GeoQuery.Builder<Builder> {

    public Builder() {
      super(Type.NODE);
    }

    public NodeQuery build() {
      return new NodeQuery(this);
    }

    @Override
    protected Builder self() {
      validate();
      return this;
    }

    @Override
    protected void validate() {
      super.validate();
      if (selection == Selection.FORWARD_NODE
          || selection == Selection.BACKWARD_NODE
          || selection == Selection.BACKWARD_WAY
          || selection == Selection.BACKWARD_RELATION) {
        throw new IllegalArgumentException(EX_WRONG_SCHEMA);
      }
    }
  }
}
