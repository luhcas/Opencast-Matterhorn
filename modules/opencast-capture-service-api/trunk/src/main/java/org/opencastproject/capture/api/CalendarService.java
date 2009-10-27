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

import java.net.URI;

/**
 * OSGi service for fetching calendar data captures (MH-1052) 
 */
public interface CalendarService {

  /**
   * Sets up the calendar service and the internal polling system
   * @param uri            The URI to pull the calendaring data from
   * @param pollingTime    The time between pulls from the server
   */
  public void init(URI uri, int pollingTime);
  
  /**
   * Updates the calendar from the
   * @param url The URI to pull the calendaring data from .  Set this to null if you don't want to change the URL
   */
  public void updateCalendar(URI uri);

  /**
   * Enables polling for new calendar data
   */
  public void enablePolling();

  /**
   * Disables polling for new calendar data
   */
  public void disablePolling();

  /**
   * Shuts down the polling system.  This should only be called when shutting down the OSGi bundle.
   * @param wait Should the scheduler wait for its currently running jobs to finish before shutting down.
   */
  public void shutdownPolling(boolean wait);
}
