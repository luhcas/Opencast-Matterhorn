/**
 *  Copyright 2009, 2010 The Regents of the University of California
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
  void setScheduleEndpoint(URL url);

  /**
   * Gets the current schedule data URL.  This should be an endpoint which generates iCal (RFC 2445) format scheduling data.
   * @return The current schedule data URL.
   */
  URL getScheduleEndpoint();

  /**
   * Polls the current schedule endpoint URL for new scheduling data.
   * If the new schedule data contains an error or is unreachable the previous recording schedule is used instead.
   */
  void updateCalendar();

  /**
   * Gets the time between refreshes of the scheduling data.
   * @return The number of seconds between refreshes of the scheduling data.
   */
  int getPollingTime();

  /**
   * Enables polling for new calendar data.
   * @param enable True to enable polling, false otherwise.
   */
  void enablePolling(boolean enable);

  /**
   * Checks to see if the is set to automatically poll for new scheduling data.
   * @return True if the system is set to poll for new data, false otherwise.
   */
  boolean isPollingEnabled();

  /**
   * Starts the scheduling system.  Calling this enables scheduled captures.
   */
  void startScheduler();

  /**
   * Checks to see if the system is set to capture from its calendar data.
   * @return True if the system is set to capture from a schedule, false otherwise.
   */
  boolean isSchedulerEnabled();

  /**
   * Stops the scheduling system.  Calling this disables scheduled captures.
   */
  void stopScheduler();
}
