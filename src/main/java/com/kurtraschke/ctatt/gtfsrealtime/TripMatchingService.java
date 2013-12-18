/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kurtraschke.ctatt.gtfsrealtime;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import com.google.common.collect.Iterables;
import com.kurtraschke.ctatt.gtfsrealtime.api.Eta;
import com.kurtraschke.ctatt.gtfsrealtime.gtfs.ScheduledTrip;

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
 * - direction of travel (as determined by terminal station)
 * -
 *
 *
 * @author kurt
 */
@Singleton
public class TripMatchingService {

  @Inject
  GtfsDaoService daoService;
  String agencyId = "CTA";

  public Trip matchTrip(String runNumber, String routeName, String destinationStationId, String destinationStationName, List<Eta> etas) throws TripMatchingException {
    GtfsRelationalDao dao = daoService.getGtfsRelationalDao();

    Collection<ScheduledTrip> candidateTrips = daoService.getScheduledTripMapping().get("R" + runNumber);

    List<Trip> rightRouteTrips = new ArrayList<>();


    for (ScheduledTrip candidateTrip : candidateTrips) {
      Trip t = dao.getTripForId(new AgencyAndId(agencyId, candidateTrip.tripId));

      if (t.getRoute().getId().getId().equalsIgnoreCase(routeName)) {
        rightRouteTrips.add(t);
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

    Calendar c = new GregorianCalendar(TimeZone.getTimeZone("America/Chicago"));

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

    System.out.println(runNumber + " " + bestMatch.getMinValue());

    return bestMatch.getMinElement();
  }

  private double delta(ServiceDate base, List<StopTime> scheduled, List<Eta> actual) throws TripMatchingException {
    double deltaSum = 0;

    long baseTime = base.getAsDate(TimeZone.getTimeZone("America/Chicago")).getTime() / 1000L;

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