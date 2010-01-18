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
   * Sets the schedule data URL from which to gather scheduling data.
   * @param url The URL to pull the calendaring data from
   */
  //TODO: what should this url point to?  an ical feed?  if so, indicate the format of the feed (e.g. just the ical spec?  identify which spec)
  public void setScheduleEndpoint(URL url);

  /**
   * Gets the current schedule data URL
   * @return The current schedule data URL
   */
  //TODO: what should this url point to?  an ical feed?  if so, indicate the format of the feed (e.g. just the ical spec?  identify which spec)
  public URL getScheduleEndpoint();

  /**
   * Polls the current schedule endpoint URL for new scheduling data
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
  //TODO: indicate the unit of time in the @return (seconds)
  public long getPollingTime();

  /**
   * Enables polling for new calendar data
   */
  public void enablePolling();

  /**
   * Checks to see if the system is polling for new calendar data
   * @return True if the system is set to poll for new data, false otherwise
   */
  //TODO: does this mean the client is currently in the middle of the poll, or whether the client will periodically poll?  It sounds like the former, but I think it should be the latter.  If it should be the latter, it should be renamed to something like isPollingEnabled()
  public boolean isPolling();

  /**
   * Disables polling for new calendar data
   */
  //TODO: why two methods?  enablePolling(false) seems just as clear and is one less api item to read
  public void disablePolling();

  /**
   * Starts the scheduling system.  Calling this enables scheduled captures.
   */
  //TODO: refactor this to startScheduler() instead.
  public void enableScheduler();

  /**
   * Checks to see if the system is set to capture from its calendar data
   * @return True if the system is set to capture from a schedule, false otherwise
   */
  public boolean isEnabled();

  /**
   * Stops the scheduling system.  Calling this disables scheduled captures.
   */
  //TODO: refactor this to stopScheduler() instead
  public void disableScheduler();
}
