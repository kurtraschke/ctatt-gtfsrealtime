/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kurtraschke.ctatt.gtfsrealtime.api;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 *
 * @author kurt
 */
public class Route {

  @JacksonXmlProperty(localName = "name", isAttribute = true)
  public String name;
  @JacksonXmlProperty(localName = "train")
  public List<Train> trains;
}
