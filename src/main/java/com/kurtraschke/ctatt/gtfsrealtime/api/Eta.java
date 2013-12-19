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
package com.kurtraschke.ctatt.gtfsrealtime.api;

import java.text.ParseException;
import java.util.Date;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.kurtraschke.ctatt.gtfsrealtime.DateUtils;

/**
 *
 * @author kurt
 */
@JacksonXmlRootElement(localName = "eta")
public class Eta {

  @JacksonXmlProperty(localName = "rn")
  public String runNumber;
  @JacksonXmlProperty(localName = "destSt")
  public String destinationStation;
  @JacksonXmlProperty(localName = "destNm")
  public String destinationName;
  @JacksonXmlProperty(localName = "trDr")
  public String trainDirection;
  @JacksonXmlProperty(localName = "staId")
  public String stationId;
  @JacksonXmlProperty(localName = "stpId")
  public String stopId;
  @JacksonXmlProperty(localName = "staNm")
  public String stationName;
  public Date predictionTimestamp;
  public Date arrivalTime;
  @JacksonXmlProperty(localName = "isApp")
  public int isApproaching;
  @JacksonXmlProperty(localName = "isDly")
  public int isDelayed;
  @JacksonXmlProperty(localName = "isSch")
  public int isSchedule;
  @JacksonXmlProperty(localName = "isFlt")
  public int isFaulty;
  @JacksonXmlProperty(localName = "flags")
  public String flags;
  @JacksonXmlProperty(localName = "stpDe")
  public String platform;
  @JacksonXmlProperty(localName = "rt")
  public String route;

  @JacksonXmlProperty(localName = "prdt")
  public void setPredictionTimestamp(String predictionTimestamp) throws ParseException {
    this.predictionTimestamp = DateUtils.parseTimestamp(predictionTimestamp);
  }

  @JacksonXmlProperty(localName = "arrT")
  public void setArrivalTime(String arrivalTime) throws ParseException {
    this.arrivalTime = DateUtils.parseTimestamp(arrivalTime);
  }
}
