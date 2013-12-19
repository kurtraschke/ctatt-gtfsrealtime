/*
 * Copyright (C) 2013 Kurt Raschke
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
