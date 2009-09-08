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
package org.opencastproject.workflow.api;

import org.opencastproject.media.mediapackage.MediaPackage;

import java.util.Map;

/**
 * An single instance of a running, paused, or stopped workflow.
 */
public interface WorkflowInstance {
  public enum State {RUNNING, STOPPED, PAUSED}
  
  /**
   * The unique ID of this {@link WorkflowInstance}
   */
  String getId();
  
  /**
   * The short title of this {@link WorkflowInstance}
   */
  String getTitle();

  /**
   * The longer description of this {@link WorkflowInstance}
   */
  String getDescription();

  /**
   * The {@link WorkflowDefinition} that served as a template for this {@link WorkflowInstance} the moment the instance
   * was created.
   */
  public WorkflowDefinition getWorkflowDefinition();

  /**
   * The current {@link State} of this {@link WorkflowInstance}
   */
  public State getState();
  
  /**
   * The {@link MediaPackage} associated with this {@link WorkflowInstance}
   */
  public MediaPackage getMediaPackage();
  
  /**
   * The properties associated with this workflow instance.  Properties can be used to affect how
   * {@link WorkflowDefinition#getOperations()} are eventually run.
   */
  public Map<String, String> getProperties();
}

