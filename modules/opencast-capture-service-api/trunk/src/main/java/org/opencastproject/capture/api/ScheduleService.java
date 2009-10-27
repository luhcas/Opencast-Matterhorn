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

/**
 * OSGi service for scheduling captures (MH-1052) 
 */
public interface ScheduleService {

  /**
   * Starts the cron4j system.  Calling this enables scheduled captures.
   */
  public void enableScheduler();

  /**
   * Stops the cron4j system.  Calling this disables scheduled captures.
   */
  public void disableScheduler();

  /**
   * Shuts down the scheduler.  This should only be called when shutting down the OSGi bundle.
   * @param wait Should the scheduler wait for its currently running jobs to finish before shutting down.
   */
  public void shutdownScheduler(boolean wait);
}
