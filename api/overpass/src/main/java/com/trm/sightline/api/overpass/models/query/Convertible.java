package com.trm.sightline.api.overpass.models.query;

public interface Convertible<B> {
  B toBuilder();

  String toQuery();
}
