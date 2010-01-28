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
package org.opencastproject.composer.impl.endpoint;

import org.opencastproject.media.mediapackage.Track;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A receipt for an encoding job.  A Receipt may be used to track an encoding job once it has been queued.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="receipt", namespace="http://composer.opencastproject.org/")
public class Receipt {
  private static final Logger logger = LoggerFactory.getLogger(Receipt.class);

  public static enum STATUS {RUNNING, FINISHED, FAILED}

  public Receipt() {}

  public Receipt(String id, String status) {
    this.id = id;
    this.status = status;
  }

  public Receipt(String id, String status, Track track) {
    this(id, status);
    this.track = track;
  }

  @XmlID
  @XmlAttribute
  String id;
  
  @XmlAttribute
  String status;
  
  @XmlElement
  Track track;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Track getTrack() {
    return track;
  }

  public void setTrack(Track track) {
    this.track = track;
  }
}
