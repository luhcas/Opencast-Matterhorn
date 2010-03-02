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
 * Interface to OSGi service for fetching capture schedules and starting captures (MH-1052).
 */
 
public interface Scheduler {

  /**
   * Sets the schedule data URL from which to gather scheduling data.  This should be a endpoint which generates iCal (RFC 2445) format scheduling data.
   * @param url The URL to pull the calendaring data from.
   */
  public void setScheduleEndpoint(URL url);

  /**
   * Gets the current schedule data URL.  This should be an endpoint which generates iCal (RFC 2445) format scheduling data.
   * @return The current schedule data URL.
   */
  public URL getScheduleEndpoint();

  /**
   * Polls the current schedule endpoint URL for new scheduling data.
   * If the new schedule data contains an error or is unreachable the previous recording schedule is used instead.
   */
  public void updateCalendar();

  /**
   * Gets the time between refreshes of the scheduling data.
   * @return The number of seconds between refreshes of the scheduling data.
   */
  public int getPollingTime();

  /**
   * Enables polling for new calendar data.
   * @param enable True to enable polling, false otherwise.
   */
  public void enablePolling(boolean enable);

  /**
   * Checks to see if the is set to automatically poll for new scheduling data.
   * @return True if the system is set to poll for new data, false otherwise.
   */
  public boolean isPollingEnabled();

  /**
   * Starts the scheduling system.  Calling this enables scheduled captures.
   */
  public void startScheduler();

  /**
   * Checks to see if the system is set to capture from its calendar data.
   * @return True if the system is set to capture from a schedule, false otherwise.
   */
  public boolean isSchedulerEnabled();

  /**
   * Stops the scheduling system.  Calling this disables scheduled captures.
   */
  public void stopScheduler();
}
