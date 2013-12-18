/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

  @CsvField(name="trip_id")
  public String tripId;
  @CsvField(name="schd_trip_id")
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
