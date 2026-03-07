package com.trm.sightline.api.overpass.models.response.adapters;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.trm.sightline.api.overpass.utils.Iso8601Utils;

import java.io.IOException;
import java.util.Date;

public class Iso8601Adapter extends JsonAdapter<Date> {
  @Override
  public synchronized Date fromJson(JsonReader reader) throws IOException {
    String string = reader.nextString();
    return string == null ? null : Iso8601Utils.parse(string);
  }

  @Override
  public synchronized void toJson(JsonWriter writer, Date value) throws IOException {
    String string = Iso8601Utils.format(value);
    writer.value(string);
  }
}
