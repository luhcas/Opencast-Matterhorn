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
package org.opencastproject.deliver.store;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity(name="TaskEntity")
@Table(name="MH_DISTRIBUTION_TASK_ENTITY")
public class TaskEntity {
  /** A no-arg constructor is required */
  public TaskEntity() {}
  
  @Id
  protected String id;

  @Column(name="serializedTask", length=1024)
  protected String serializedTask;

  @Column(name="lastModified")
  protected Long lastModified;

  @Column(name="distributionChannel")
  protected String distributionChannel;

  @Id
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getSerializedTask() {
    return serializedTask;
  }

  public void setSerializedTask(String serializedTask) {
    this.serializedTask = serializedTask;
  }

  public Long getLastModified() {
    return lastModified;
  }

  public void setLastModified(Long lastModified) {
    this.lastModified = lastModified;
  }

  public String getDistributionChannel() {
    return distributionChannel;
  }

  public void setDistributionChannel(String distributionChannel) {
    this.distributionChannel = distributionChannel;
  }
}
