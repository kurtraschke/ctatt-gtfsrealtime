/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kurtraschke.ctatt.gtfsrealtime.api;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.kurtraschke.ctatt.gtfsrealtime.DateUtils;

/**
 *
 * @author kurt
 */
@JacksonXmlRootElement(localName = "ctatt")
public class Positions {

  public Date timestamp;
  @JacksonXmlProperty(localName = "errCd")
  public int errorCode;
  @JacksonXmlProperty(localName = "errNm")
  public String errorName;
  @JacksonXmlProperty(localName = "route")
  public List<Route> routes;

  @JacksonXmlProperty(localName = "tmst")
  public void setTimestamp(String timestamp) throws ParseException {
    this.timestamp = DateUtils.parseTimestamp(timestamp);
  }
}
