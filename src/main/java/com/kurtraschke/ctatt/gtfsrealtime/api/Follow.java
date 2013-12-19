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
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.kurtraschke.ctatt.gtfsrealtime.DateUtils;

/**
 *
 * @author kurt
 */
@JacksonXmlRootElement(localName = "ctatt")
@JsonIgnoreProperties(ignoreUnknown=true)
public class Follow {

  public Date timestamp;
  @JacksonXmlProperty(localName = "errCd")
  public int errorCode;
  @JacksonXmlProperty(localName = "errNm")
  public String errorName;
  @JacksonXmlProperty(localName = "position")
  public Position position;
  @JacksonXmlProperty(localName = "eta")
  public List<Eta> etas;

  @JacksonXmlProperty(localName = "tmst")
  public void setTimestamp(String timestamp) throws ParseException {
    this.timestamp = DateUtils.parseTimestamp(timestamp);
  }
}
