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
package org.opencastproject.workflow.api;

import org.opencastproject.media.mediapackage.MediaPackage;

import java.util.Map;

public class WorkflowOperationResultImpl implements WorkflowOperationResult {
  protected MediaPackage resultingMediaPackage;

  protected Map<String, String> properties;

  protected Action action;

  protected boolean wait;

  /**
   * No arg constructor needed by JAXB
   */
  public WorkflowOperationResultImpl() {}

  /**
   * Constructs a new WorkflowOperationResultImpl from a mediapackage and an action.
   * 
   * @param resultingMediaPackage
   * @param action
   */
  public WorkflowOperationResultImpl(MediaPackage resultingMediaPackage, Map<String, String> properties, Action action) {
    this.resultingMediaPackage = resultingMediaPackage;
    this.properties = properties;
    if(action == null) {
      throw new IllegalArgumentException("action must not be null.");
    } else {
      this.action = action;
    }
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationResult#getMediaPackage()
   */
  public MediaPackage getMediaPackage() {
    return resultingMediaPackage;
  }

  /**
   * Sets the resulting media package.
   * @param mediaPackage
   */
  public void setMediaPackage(MediaPackage mediaPackage) {
    this.resultingMediaPackage = mediaPackage;
  }

  /**
   * 
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationResult#getAction()
   */
  public Action getAction() {
    return action;
  }

  /**
   * Sets the action that the workflow service should take on the workflow instance
   * @param action
   */
  public void setAction(Action action) {
    if(action == null) throw new IllegalArgumentException("action must not be null.");
    this.action = action;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationResult#getProperties()
   */
  @Override
  public Map<String, String> getProperties() {
    return properties;
  }

}
