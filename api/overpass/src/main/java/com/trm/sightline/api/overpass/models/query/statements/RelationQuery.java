package com.trm.sightline.api.overpass.models.query.statements;

import com.trm.sightline.api.overpass.models.query.statements.base.GeoQuery;
import com.trm.sightline.api.overpass.models.query.statements.base.Type;

public class RelationQuery extends GeoQuery {

  private RelationQuery(Builder builder) {
    super(builder);
  }

  public static class Builder extends GeoQuery.Builder<Builder> {

    public Builder() {
      super(Type.RELATION);
    }

    public RelationQuery build() {
      return new RelationQuery(this);
    }

    @Override
    protected Builder self() {
      validate();
      return this;
    }
  }
}
