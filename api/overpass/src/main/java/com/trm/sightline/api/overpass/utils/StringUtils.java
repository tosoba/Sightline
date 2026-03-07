package com.trm.sightline.api.overpass.utils;

import java.util.List;

public class StringUtils {

  public static String join(String delimiter, List<String> strings) {
    StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (String string : strings) {
      if (first) {
        first = false;
      } else {
        builder.append(delimiter);
      }
      builder.append(string);
    }
    return builder.toString();
  }
}
