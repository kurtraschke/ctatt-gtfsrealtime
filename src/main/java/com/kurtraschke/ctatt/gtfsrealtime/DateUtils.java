/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kurtraschke.ctatt.gtfsrealtime;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 *
 * @author kurt
 */
public class DateUtils {

  public static Date parseTimestamp(String timestamp) throws ParseException {
    TimeZone tz = TimeZone.getTimeZone("America/Chicago");

    DateFormat sdf = new SimpleDateFormat("yyyyMMdd h:m:s");
    sdf.setTimeZone(tz);

    return sdf.parse(timestamp);
  }
}
