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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Joiner;
import com.kurtraschke.ctatt.gtfsrealtime.TrainTrackerDataException;
import com.kurtraschke.ctatt.gtfsrealtime.api.Follow;
import com.kurtraschke.ctatt.gtfsrealtime.api.Positions;

/**
 *
 * @author kurt
 */
@Singleton
public class TrainTrackerDataService {

  @Inject
  @Named("CTATT.key")
  private String _trainTrackerKey;
  private HttpClientConnectionManager _connectionManager;
  private XmlMapper _xmlMapper;

  @PostConstruct
  public void start() {
    _connectionManager = new BasicHttpClientConnectionManager();
    JacksonXmlModule m = new JacksonXmlModule();
    m.setDefaultUseWrapper(false);
    _xmlMapper = new XmlMapper(m);
  }

  @PreDestroy
  public void stop() {
    _connectionManager.shutdown();
  }

  public Positions fetchAllTrains(List<String> routes) throws MalformedURLException, IOException, TrainTrackerDataException, URISyntaxException {
    URIBuilder b = new URIBuilder("http://lapi.transitchicago.com/api/1.0/ttpositions.aspx");
    b.addParameter("key", _trainTrackerKey);
    b.addParameter("rt", Joiner.on(',').join(routes));

    CloseableHttpClient client = HttpClients.custom().setConnectionManager(_connectionManager).build();

    HttpGet httpget = new HttpGet(b.build());
    try (CloseableHttpResponse response = client.execute(httpget)) {
      HttpEntity entity = response.getEntity();
      Positions p = _xmlMapper.readValue(entity.getContent(), Positions.class);

      if (p.errorCode != 0) {
        throw new TrainTrackerDataException(p.errorCode, p.errorName);
      }

      return p;
    }

  }

  public Follow fetchTrain(String runNumber) throws URISyntaxException, IOException, TrainTrackerDataException {
    URIBuilder b = new URIBuilder("http://lapi.transitchicago.com/api/1.0/ttfollow.aspx");
    b.addParameter("key", _trainTrackerKey);
    b.addParameter("runnumber", runNumber);

    CloseableHttpClient client = HttpClients.custom().setConnectionManager(_connectionManager).build();

    HttpGet httpget = new HttpGet(b.build());
    try (CloseableHttpResponse response = client.execute(httpget)) {
      HttpEntity entity = response.getEntity();
      Follow f = _xmlMapper.readValue(entity.getContent(), Follow.class);

      if (f.errorCode != 0) {
        throw new TrainTrackerDataException(f.errorCode, f.errorName);
      }

      return f;
    }
  }
}
