/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kurtraschke.ctatt.gtfsrealtime;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.Position;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.google.transit.realtime.GtfsRealtime.VehicleDescriptor;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.kurtraschke.ctatt.gtfsrealtime.api.Eta;
import com.kurtraschke.ctatt.gtfsrealtime.api.Follow;
import com.kurtraschke.ctatt.gtfsrealtime.api.Positions;
import com.kurtraschke.ctatt.gtfsrealtime.api.Route;
import com.kurtraschke.ctatt.gtfsrealtime.api.Train;

import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeGuiceBindingTypes.Alerts;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeGuiceBindingTypes.TripUpdates;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeGuiceBindingTypes.VehiclePositions;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeIncrementalUpdate;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeSink;

/**
 *
 * @author kurt
 */
class TrainTrackerRealtimeProvider {

  private static final Logger _log = LoggerFactory.getLogger(TrainTrackerRealtimeProvider.class);
  private ScheduledExecutorService _executor;
  private GtfsRealtimeSink _vehiclePositionsSink;
  private GtfsRealtimeSink _tripUpdatesSink;
  private GtfsRealtimeSink _alertsSink;
  private TripMatchingService _tms;
  private XmlMapper xmlMapper;
  @Inject
  @Named("refreshInterval.vehicles")
  private int _vehicleRefreshInterval;
  @Inject
  @Named("CTATT.key")
  private String _trainTrackerKey;

  @Inject
  public void setVehiclePositionsSink(@VehiclePositions GtfsRealtimeSink sink) {
    _vehiclePositionsSink = sink;
  }

  @Inject
  public void setTripUpdateSink(@TripUpdates GtfsRealtimeSink sink) {
    _tripUpdatesSink = sink;
  }

  @Inject
  public void setAlertsSink(@Alerts GtfsRealtimeSink sink) {
    _alertsSink = sink;
  }

  @Inject
  public void setTripMatchingService(TripMatchingService tripMatchingService) {
    this._tms = tripMatchingService;
  }

  public TrainTrackerRealtimeProvider() {
    JacksonXmlModule m = new JacksonXmlModule();
    m.setDefaultUseWrapper(false);
    xmlMapper = new XmlMapper(m);
  }

  @PostConstruct
  public void start() {
    _log.info("Starting GTFS-realtime service");
    _executor = Executors.newSingleThreadScheduledExecutor();
    _executor.scheduleWithFixedDelay(new VehiclesRefreshTask(), 0,
            _vehicleRefreshInterval, TimeUnit.SECONDS);
  }

  /**
   * The stop method cancels the recurring vehicle data downloader task.
   */
  @PreDestroy
  public void stop() {
    _log.info("Stopping GTFS-realtime service");
    _executor.shutdownNow();
  }

  private void refreshVehicles() throws MalformedURLException, IOException, TrainTrackerDataException {
    Positions p = fetchAllTrains();

    for (Route r : p.routes) {
      if (r.trains != null) {
        for (Train t : r.trains) {
          _log.info("Processing train " + t.runNumber);
          try {
            processTrain(t, r.name);
          } catch (Exception ex) {
            _log.warn("Exception while processing train " + t.runNumber, ex);
          }
        }
      }
    }
  }

  private Positions fetchAllTrains() throws MalformedURLException, IOException, TrainTrackerDataException {
    URL trainPositions = new URL("http://lapi.transitchicago.com/api/1.0/ttpositions.aspx?rt=Red,Blue,Brn,G,Org,P,Pink,Y&key=" + _trainTrackerKey);

    Positions p = xmlMapper.readValue(trainPositions.openStream(), Positions.class);

    if (p.errorCode != 0) {
      throw new TrainTrackerDataException(p.errorCode, p.errorName);
    }

    return p;

  }

  private Follow fetchTrain(String runNumber) throws MalformedURLException, IOException, TrainTrackerDataException {
    URL trainFollow = new URL("http://lapi.transitchicago.com/api/1.0/ttfollow.aspx?key=" + _trainTrackerKey + "&runnumber=" + runNumber);

    Follow f = xmlMapper.readValue(trainFollow.openStream(), Follow.class);

    if (f.errorCode != 0) {
      throw new TrainTrackerDataException(f.errorCode, f.errorName);
    }

    return f;

  }

  private void processTrain(Train train, String route) throws MalformedURLException, IOException, TrainTrackerDataException, TripMatchingException {
    List<Eta> etas = null;

    try {
      Follow f = fetchTrain(train.runNumber);
      etas = f.etas;
    } catch (TrainTrackerDataException ex) {
      _log.warn("Falling back to single-station prediction for train " + train.runNumber, ex);
    }

    if (etas == null || etas.isEmpty()) {
      Eta e = new Eta();

      e.predictionTimestamp = train.predictionTimestamp;
      e.arrivalTime = train.arrivalTime;
      e.flags = train.flags;
      e.isApproaching = train.isApproaching;
      e.isDelayed = train.isDelayed;
      e.stationId = train.nextStationId;
      e.stationName = train.nextStationName;
      e.stopId = train.nextStopId;

      etas = Collections.singletonList(e);

    }

    TripDescriptor td = tripDescriptorForTrain(train, etas, route);
    VehicleDescriptor vd = vehicleDescriptorForTrain(train);
    Position pos = positionForTrain(train);

    TripUpdate.Builder tu = TripUpdate.newBuilder();
    VehiclePosition.Builder vp = VehiclePosition.newBuilder();

    vp.setTrip(td);
    vp.setVehicle(vd);
    vp.setTimestamp(train.predictionTimestamp.getTime() / 1000L);
    vp.setPosition(pos);

    tu.setTrip(td);
    tu.setVehicle(vd);
    tu.setTimestamp(train.predictionTimestamp.getTime() / 1000L);

    for (Eta e : etas) {
      if (e.isFaulty == 1 || e.isSchedule == 1) {
        continue;
      }

      StopTimeUpdate st = stopTimeUpdateForEta(e);
      tu.addStopTimeUpdate(st);
    }

    pushEntity(train.runNumber, _tripUpdatesSink, tu.build(), FeedEntity.TRIP_UPDATE_FIELD_NUMBER);
    pushEntity(train.runNumber, _vehiclePositionsSink, vp.build(), FeedEntity.VEHICLE_FIELD_NUMBER);
  }

  private void pushEntity(String id, GtfsRealtimeSink sink, Object value, int field) {
    GtfsRealtimeIncrementalUpdate griu = new GtfsRealtimeIncrementalUpdate();

    FeedEntity.Builder feb = FeedEntity.newBuilder();

    feb.setId(id);
    feb.setField(FeedEntity.getDescriptor().findFieldByNumber(field), value);

    griu.addUpdatedEntity(feb.build());

    sink.handleIncrementalUpdate(griu);
  }

  private TripDescriptor tripDescriptorForTrain(Train train, List<Eta> etas, String route) throws TripMatchingException {
    TripDescriptor.Builder b = TripDescriptor.newBuilder();

    Trip matchedTrip = _tms.matchTrip(train.runNumber, route, train.destinationStation, train.destinationName, etas);

    b.setRouteId(matchedTrip.getRoute().getId().getId());
    b.setTripId(matchedTrip.getId().getId());

    return b.build();
  }

  private VehicleDescriptor vehicleDescriptorForTrain(Train train) {
    VehicleDescriptor.Builder b = VehicleDescriptor.newBuilder();

    b.setId("R" + train.runNumber);
    b.setLabel("R" + train.runNumber);

    return b.build();
  }

  private Position positionForTrain(Train train) {
    Position.Builder b = Position.newBuilder();

    b.setLatitude((float) train.latitude);
    b.setLongitude((float) train.longitude);
    b.setBearing(train.heading);

    return b.build();
  }

  private StopTimeUpdate stopTimeUpdateForEta(Eta e) {
    StopTimeUpdate.Builder b = StopTimeUpdate.newBuilder();

    b.setStopId(e.stopId);
    StopTimeEvent.Builder arrivalBuilder = b.getArrivalBuilder();
    arrivalBuilder.setTime(e.arrivalTime.getTime() / 1000L);

    return b.build();
  }

  private class VehiclesRefreshTask implements Runnable {

    @Override
    public void run() {
      try {
        _log.info("Refreshing vehicles");
        refreshVehicles();
      } catch (Exception ex) {
        _log.warn("Error in vehicle refresh task", ex);
      }
    }
  }
}
