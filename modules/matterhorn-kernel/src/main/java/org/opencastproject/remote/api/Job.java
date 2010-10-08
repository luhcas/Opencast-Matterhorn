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
package org.opencastproject.remote.api;

import org.opencastproject.mediapackage.MediaPackageElement;

import java.util.Date;

/**
 * A receipt for an long running, asynchronous job. A Receipt may be used to track any task once it has been queued.
 */
public interface Job {
  /** The status of the job that this receipt represents */
  static enum Status {
    QUEUED, RUNNING, FINISHED, FAILED
  }

  /** Gets the receipt identifier */
  String getId();

  /** Sets the receipt identifier */
  void setId(String id);

  /** Gets the job type, which determines the type of service that runs the job */
  String getJobType();

  /** Gets the receipt's current {@link Status} */
  Status getStatus();

  /** Sets the receipt's current {@link Status} */
  void setStatus(Status status);

  /** Gets the host responsible for queuing and running this job */
  String getHost();

  /** The date this receipt was created */
  Date getDateCreated();

  /** The date this job was started. If the job was queued, this can be significantly later than the date created. */
  Date getDateStarted();

  /** The date this job was completed */
  Date getDateCompleted();

  /**
   * Gets the mediapackage element that was produced by this job, or null if none was produced, or if it has yet to be
   * produced.
   * 
   * @return the mediapackage element
   */
  MediaPackageElement getElement();

  /** Sets the mediapackage element produced by this job. */
  void setElement(MediaPackageElement element);

  /** Gets an xml representation of this receipt */
  String toXml();
}
