/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kurtraschke.ctatt.gtfsrealtime;

/**
 *
 * @author kurt
 */
public class TrainTrackerDataException extends Exception {

  public TrainTrackerDataException() {
  }

  public TrainTrackerDataException(int errorCode, String errorName) {
    super("Train Tracker API error: " + errorCode + " " + errorName);
  }
}
