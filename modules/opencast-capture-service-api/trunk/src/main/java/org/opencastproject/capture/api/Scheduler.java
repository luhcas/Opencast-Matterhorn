/**
 *  Copyright 2009 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.capture.api;

import java.net.URL;

/**
 * Interface to OSGi service for fetching capture schedules and starting captures (MH-1052) 
 */
 
public interface Scheduler {

  /**
   * Sets up the calendar service and the internal polling system
   * Note that if this method encounters an error it will return, and no schedulers will be running
   * In effect, the capture device will fall back to manual mode and no scheduled captures will take place
   */
  public void init();
  
  /**
   * Sets the schedule data URL form which to gather scheduling data
   * @param url The URL to pull the calendaring data from
   */
  public void setScheduleEndpoint(URL url);

  /**
   * Gets the current schedule data URL
   * @return The current schedule data URL
   */
  public URL getScheduleEndpoint();

  /**
   * Polls the current schedule data URL for new scheduling data
   * If the new schedule data contains an error or is unreachable the previous recording schedule is used instead
   */
  public void updateCalendar();

  /**
   * Sets the time between refreshes of the scheduling data
   * @param pollingTime The time between polls, in seconds
   */
  public void setPollingTime(int pollingTime);

  /**
   * Gets the time between refreshes of the scheduling data
   * @return The time between refreshes of the scheduling data
   */
  public int getPollingTime();

  /**
   * Enables polling for new calendar data
   */
  public void enablePolling();

  /**
   * Checks to see if the system is polling for new calendar data
   * @return True if the system is set to poll for new data, false otherwise
   */
  public boolean isPolling();

  /**
   * Disables polling for new calendar data
   */
  public void disablePolling();

  /**
   * Starts the scheduling system.  Calling this enables scheduled captures.
   */
  public void enableScheduler();

  /**
   * Checks to see if the system is set to capture from its calendar data
   * @return True if the system is set to capture from a schedule, false otherwise
   */
  public boolean isEnabled();

  /**
   * Stops the scheduling system.  Calling this disables scheduled captures.
   */
  public void disableScheduler();
}
