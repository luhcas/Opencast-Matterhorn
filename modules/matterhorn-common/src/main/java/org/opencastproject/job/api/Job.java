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
package org.opencastproject.job.api;

import java.util.Date;
import java.util.List;

/**
 * Represents a long running, asynchronous process. A Job may be used to track any task, whether it is queued to run in
 * the future, currently running, or has run in the past.
 */
public interface Job {

  /** The status of the job that this receipt represents */
  static enum Status {
    QUEUED, PAUSED, RUNNING, FINISHED, FAILED, DELETED
  }

  /**
   * Gets the job identifier.
   * 
   * @return the identifier
   */
  long getId();

  /**
   * Gets the version of this job. Each time the job is updated, the version number is incremented. If a process
   * attempts to save a job that has been updated in another thread or on another host while the job was in memory, an
   * optimistic locking exception will be thrown.
   * 
   * @return the version number of this job
   */
  int getVersion();

  /**
   * Sets the job identifier.
   * 
   * @param id
   *          the job identifier
   */
  void setId(long id);

  /**
   * Gets the job type, which determines the type of service that runs the job.
   * 
   * @return the job type
   */
  String getJobType();

  /**
   * The operation type, which can be used by the service responsible for the job to determine the service method to
   * execute.
   * 
   * @return The operation
   */
  String getOperationType();

  /**
   * The arguments passed to the service and operation. Each argument must be serializable to a string.
   * 
   * @return the arguments passed to the service operation
   */
  List<String> getArguments();

  /**
   * Gets the receipt's current {@link Status}
   * 
   * @return the current status
   */
  Status getStatus();

  /**
   * Sets the receipt's current {@link Status}.
   * 
   * @param status
   *          the status to set
   */
  void setStatus(Status status);

  /**
   * Gets the host that created this job.
   * 
   * @return the server that originally created the job
   */
  String getCreatedHost();

  /**
   * Gets the host responsible for running this job.
   * 
   * @return the server running the job, or null if the job hasn't yet started
   */
  String getProcessingHost();

  /**
   * The date this receipt was created.
   * 
   * @return the date the job was created
   */
  Date getDateCreated();

  /**
   * The date this job was started. If the job was queued, this can be significantly later than the date created.
   * 
   * @return the date the job was started
   */
  Date getDateStarted();

  /**
   * The date this job was completed
   * 
   * @return the date completed
   */
  Date getDateCompleted();

  /**
   * Gets the serialized output that was produced by this job, or null if nothing was produced, or if it has yet to be
   * produced.
   * 
   * @return the output of the job
   */
  String getPayload();

  /**
   * Sets the payload produced by this job.
   * 
   * @param payload
   *          the result of the job to store in the job
   */
  void setPayload(String payload);

}
