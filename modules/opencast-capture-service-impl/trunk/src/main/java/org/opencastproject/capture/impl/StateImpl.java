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
package org.opencastproject.capture.impl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.opencastproject.capture.api.State;

/**
 * TODO: Comment me!
 *
 */
@XmlType(name="status-entity", namespace="http://status.opencastproject.org/")
@XmlRootElement(name="status-entity", namespace="http://status.opencastproject.org/")
@XmlEnum(String.class)
@XmlAccessorType(XmlAccessType.FIELD)
public enum StateImpl implements State {
  IDLE("Idle"),
  CAPTURING("Caturing"),
  UPLOADING("Uploading");

  @XmlElement(name="status")
  private String message = null;
  
  private StateImpl(String message) {
    this.message = message;
  }
  
  public String getMessage() {
    return message;
  }
}
