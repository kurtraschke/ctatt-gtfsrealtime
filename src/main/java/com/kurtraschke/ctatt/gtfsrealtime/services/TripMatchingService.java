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
package com.kurtraschke.ctatt.gtfsrealtime.services;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.kurtraschke.ctatt.gtfsrealtime.TripMatchingException;
import com.kurtraschke.ctatt.gtfsrealtime.api.Eta;

import org.onebusaway.collections.MappingLibrary;
import org.onebusaway.collections.Min;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.GtfsRelationalDao;

/**
 * To identify trips, we get a "run number", which maps to multiple
 * GTFS trips.
 *
 * We then narrow down the GTFS trips by:
 * - route ID
 * - direction of travel (as determined by terminal station)
 * - active service IDs
 *
 * @author kurt
 */
@Singleton
public class TripMatchingService {

  @Inject
  GtfsDaoService daoService;

  public Trip matchTrip(String runNumber, String routeName, String destinationStationId, String destinationStationName, List<Eta> etas) throws TripMatchingException {
    GtfsRelationalDao dao = daoService.getGtfsRelationalDao();

    Collection<Trip> candidateTrips = daoService.getScheduledTripMapping().get("R" + runNumber);

    List<Trip> rightRouteTrips = new ArrayList<>();

    for (Trip candidateTrip : candidateTrips) {
      if (candidateTrip.getRoute().getId().getId().equalsIgnoreCase(routeName)) {
        rightRouteTrips.add(candidateTrip);
      }
    }

    if (rightRouteTrips.isEmpty()) {
      throw new TripMatchingException("No trips on route found.");
    }

    List<Trip> rightDirectionTrips = new ArrayList<>();

    for (Trip t : rightRouteTrips) {
      List<StopTime> stopTimes = dao.getStopTimesForTrip(t);

      if (Iterables.getLast(stopTimes).getStop().getName().equals(destinationStationName)
              || Iterables.getLast(stopTimes).getStop().getId().getId().equals(destinationStationId)) {
        rightDirectionTrips.add(t);
      }
    }

    if (rightDirectionTrips.isEmpty()) {
      throw new TripMatchingException("No trips to destination found.");
    }
    
    Calendar c = new GregorianCalendar(daoService.getAgencyTimeZone());

    ServiceDate today = new ServiceDate(c);
    CalendarServiceData csd = daoService.getCalendarServiceData();

    Set<AgencyAndId> serviceIds = csd.getServiceIdsForDate(today);

    List<Trip> activeTrips = new ArrayList<>();

    for (Trip t : rightDirectionTrips) {
      if (serviceIds.contains(t.getServiceId())) {
        activeTrips.add(t);
      }
    }

    if (activeTrips.isEmpty()) {
      throw new TripMatchingException("No active trips found.");
    }

    Min<Trip> bestMatch = new Min<>();

    for (Trip t : activeTrips) {
      try {
        bestMatch.add(delta(today, dao.getStopTimesForTrip(t), etas), t);
      } catch (TripMatchingException ex) {
      }
    }

    return bestMatch.getMinElement();
  }

  private double delta(ServiceDate base, List<StopTime> scheduled, List<Eta> actual) throws TripMatchingException {
    double deltaSum = 0;

    long baseTime = base.getAsDate(daoService.getAgencyTimeZone()).getTime() / 1000L;

    Map<String, List<StopTime>> stopTimeMap = MappingLibrary.mapToValueList(scheduled, "stop.id.id");

    int comparedStops = 0;

    for (Eta e : actual) {
      List<StopTime> possibleStopTimes = stopTimeMap.get(e.stopId);

      if (possibleStopTimes == null || possibleStopTimes.isEmpty()) {
        continue;
      }

      Min<StopTime> minStopTime = new Min<>();

      for (StopTime scheduledStopTime : possibleStopTimes) {
        long arrivalTime = e.arrivalTime.getTime() / 1000L;

        minStopTime.add(Math.abs(
                (scheduledStopTime.getArrivalTime() + baseTime) - arrivalTime), scheduledStopTime);
      }
      deltaSum += minStopTime.getMinValue();
      comparedStops++;
    }

    if (comparedStops == 0) {
      throw new TripMatchingException("No compared stops.");
    }

    return deltaSum / comparedStops;
  }
}
