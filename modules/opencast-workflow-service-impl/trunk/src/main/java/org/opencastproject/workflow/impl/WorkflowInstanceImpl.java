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
package org.opencastproject.workflow.impl;

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.impl.runner.AbstractWorkflowOperationRunner;

import java.util.List;
import java.util.Map;

/**
 * A simple POJO-based implementation of a {@link WorkflowDefinition}
 */
public class WorkflowInstanceImpl implements WorkflowInstance {
  private String id;
  private WorkflowDefinition workflowDefinition;
  private Map<String, String> properties;
  private MediaPackage mediaPackage;
  private String title;
  private String description;
  private State state;
  private List<? extends AbstractWorkflowOperationRunner> runners;
  
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }
  
  public void setTitle(String title) {
    this.title = title;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }

  public WorkflowDefinition getWorkflowDefinition() {
    return workflowDefinition;
  }

  public void setWorkflowDefinition(WorkflowDefinition workflowDefinition) {
    this.workflowDefinition = workflowDefinition;
  }

  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }

  public MediaPackage getMediaPackage() {
    return mediaPackage;
  }

  public void setMediaPackage(MediaPackage mediaPackage) {
    this.mediaPackage = mediaPackage;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public List<? extends AbstractWorkflowOperationRunner> getRunners() {
    return runners;
  }

  public void setRunners(List<? extends AbstractWorkflowOperationRunner> runners) {
    this.runners = runners;
  }

}

