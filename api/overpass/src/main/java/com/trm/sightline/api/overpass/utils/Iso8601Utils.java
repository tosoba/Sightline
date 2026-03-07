package com.trm.sightline.api.overpass.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Utilities methods for manipulating dates in iso8601 format.
 *
 * <p>Supported parse format: [yyyy-MM-ddThh:mm:ssZ]
 */
public final class Iso8601Utils {
  private static final String UTC_ID = "UTC";
  private static final TimeZone TIMEZONE_Z = TimeZone.getTimeZone(UTC_ID);
  private static final String ISO8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'";

  public static Date parse(String iso8601String) {
    SimpleDateFormat formatter = new SimpleDateFormat(ISO8601, Locale.getDefault());
    formatter.setTimeZone(TIMEZONE_Z);
    try {
      return formatter.parse(iso8601String);
    } catch (ParseException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static String format(Date iso8601Date) {
    SimpleDateFormat formatter = new SimpleDateFormat(ISO8601, Locale.getDefault());
    formatter.setTimeZone(TIMEZONE_Z);
    return formatter.format(iso8601Date);
  }
}
