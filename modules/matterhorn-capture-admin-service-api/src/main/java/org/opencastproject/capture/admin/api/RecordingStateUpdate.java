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
package org.opencastproject.capture.admin.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A representation of an recording which stores its id, state and time-since-last-update value
 */
@XmlType(name="recording-state-update", namespace="http://capture.admin.opencastproject.org")
@XmlRootElement(name="recording-state-update", namespace="http://capture.admin.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class RecordingStateUpdate {

  /**
   * The recording's ID.
   */
  @XmlElement(name="name")
  public String id;
  
  /**
   * The state of the recording.  This should be defined from {@link org.opencastproject.capture.admin.api.RecordingState}.
   * @see RecordingState
   */
  @XmlElement(name="state")
  public String state;
  
  /**
   * The number of milliseconds since the last time the recording checked in.  Note that this is relative (ie, it's been 3000 ms) rather than absolute (milliseconds since 1970).
   */
  @XmlElement(name="time-since-last-update")
  public Long time_since_last_update;

  /**
   * Required zero-arg. constructor. Do not use
   */
  public RecordingStateUpdate() {}
  
  /**
   * Builds an RecordingStateUpdate object about the Recording r.  This calculates the time delta for you.
   *
   * @param r The recording you wish to know more information about
   */
  public RecordingStateUpdate(Recording r) {
    id = r.getID();
    state = r.getState();
    time_since_last_update = System.currentTimeMillis() - r.getLastCheckinTime();
  }
}
