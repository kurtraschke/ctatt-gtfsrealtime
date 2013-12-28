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

import java.io.File;
import java.io.IOException;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.TimeZone;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import com.kurtraschke.ctatt.gtfsrealtime.gtfs.ScheduledTrip;

import org.onebusaway.csv_entities.schema.DefaultEntitySchemaFactory;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.impl.calendar.CalendarServiceDataFactoryImpl;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.serialization.GtfsEntitySchemaFactory;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.onebusaway.gtfs.services.calendar.CalendarServiceDataFactory;

/**
 *
 * @author kurt
 */
@Singleton
public class GtfsDaoService {

  private File _gtfsPath;
  private String _agencyId;
  private GtfsMutableRelationalDao dao;
  private CalendarServiceData csd;
  private Multimap<String, Trip> scheduledTripMapping;

  public GtfsDaoService() {
    scheduledTripMapping = ArrayListMultimap.create();
  }

  @Inject
  public void setGtfsPath(@Named("GTFS.path") File gtfsPath) {
    _gtfsPath = gtfsPath;
  }

  public GtfsRelationalDao getGtfsRelationalDao() {
    return dao;
  }

  public CalendarServiceData getCalendarServiceData() {
    return csd;
  }

  public Multimap<String, Trip> getScheduledTripMapping() {
    return ImmutableListMultimap.copyOf(scheduledTripMapping);
  }

  public TimeZone getAgencyTimeZone() {
    return csd.getTimeZoneForAgencyId(_agencyId);
  }

  public String getAgencyId() {
    return _agencyId;
  }

  @Inject
  public void setAgencyId(@Named("GTFS.agencyId") String agencyId) {
    _agencyId = agencyId;
  }

  @PostConstruct
  public void start() throws IOException {
    dao = new GtfsRelationalDaoImpl();

    DefaultEntitySchemaFactory factory = GtfsEntitySchemaFactory.createEntitySchemaFactory();
    factory.addExtension(Trip.class, ScheduledTrip.class);
    GtfsReader reader = new GtfsReader();
    reader.setEntitySchemaFactory(factory);
    reader.setInputLocation(_gtfsPath);
    reader.setEntityStore(dao);
    reader.run();
    CalendarServiceDataFactory csdf = new CalendarServiceDataFactoryImpl(dao);
    csd = csdf.createData();

    for (Trip t : dao.getAllTrips()) {
      String scheduledTripId = t.getExtension(ScheduledTrip.class).getScheduledTripId();

      scheduledTripMapping.put(scheduledTripId, t);
    }
  }
}
