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
package com.kurtraschke.ctatt.gtfsrealtime.gtfs;

import java.util.Objects;
import org.onebusaway.csv_entities.schema.annotations.CsvField;
import org.onebusaway.csv_entities.schema.annotations.CsvFields;

/**
 *
 * @author kurt
 */
@CsvFields(filename = "trips.txt")
public class ScheduledTrip {

  @CsvField(name = "trip_id")
  public String tripId;
  @CsvField(name = "schd_trip_id")
  public String scheduledTripId;

  public String getTripId() {
    return tripId;
  }

  public void setTripId(String tripId) {
    this.tripId = tripId;
  }

  public String getScheduledTripId() {
    return scheduledTripId;
  }

  public void setScheduledTripId(String scheduledTripId) {
    this.scheduledTripId = scheduledTripId;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 97 * hash + Objects.hashCode(this.tripId);
    hash = 97 * hash + Objects.hashCode(this.scheduledTripId);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final ScheduledTrip other = (ScheduledTrip) obj;
    if (!Objects.equals(this.tripId, other.tripId)) {
      return false;
    }
    if (!Objects.equals(this.scheduledTripId, other.scheduledTripId)) {
      return false;
    }
    return true;
  }
}
