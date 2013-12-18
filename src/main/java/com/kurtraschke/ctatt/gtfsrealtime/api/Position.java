/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kurtraschke.ctatt.gtfsrealtime.api;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 *
 * @author kurt
 */
@JacksonXmlRootElement(localName = "eta")
public class Position {

  @JacksonXmlProperty(localName = "lat")
  public double latitude;
  @JacksonXmlProperty(localName = "lon")
  public double longitude;
  @JacksonXmlProperty(localName = "heading")
  public int heading;
}
