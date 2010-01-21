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
package org.opencastproject.capture.admin.api;

/**
 * An in-memory construct to represent the state of a recording, and when it was last heard from 
 */
public class Recording {

  /**
   * The id of the recording
   */
  public String id;

  /**
   * The state of the recording.  This should be defined from the constants in RecordingState
   * @see org.opencastproject.capture.api.RecordingState
   */
  public String state;

  /**
   * The time at which the recording last checked in with this service.
   * Note that this is an absolute timestamp (ie, milliseconds since 1970) rather than a relative timestamp (ie, it's been 3000 ms since it last checked in) 
   */
  public Long lastHeardFrom;

  /**
   * Builds a representation of the recording
   * @param id The name of the recording
   * @param recordingState The state of the recording
   */
  public Recording(String recordingID, String recordingState) {
    id = recordingID;
    this.setState(recordingState);
  }

  /**
   * Gets the ID of the recording
   * @return The ID of the recording
   */
  public String getID() {
    return id;
  }

  /**
   * Sets the state of the recording, and updates the time it was last heard from
   * 
   * @see org.opencastproject.capture.api.RecordingState
   * @param newState The new state of the recording.  This can be equal to the current one if the goal is to update the timestamp.
   */
  public void setState(String newState) {
    state = newState;
    lastHeardFrom = System.currentTimeMillis();
  }

  /**
   * Gets the state of the recording
   * 
   * @see org.opencastproject.capture.api.RecordingState
   * @return The state of the recording as defined in RecordingState
   */

  public String getState() {
    return state;
  }

  /**
   * Gets the time at which the recording last checked in
   * @return The number of milliseconds since 1970 when the recording last checked in
   */
  public Long getLastCheckinTime() {
    return lastHeardFrom;
  }
}
