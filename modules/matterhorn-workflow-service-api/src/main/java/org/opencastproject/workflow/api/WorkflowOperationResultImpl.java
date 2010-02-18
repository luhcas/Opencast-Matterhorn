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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * A JAXB-annotated implementation of {@link WorkflowOperationResult}
 */
@XmlType(name="operation-result", namespace="http://workflow.opencastproject.org/")
@XmlRootElement(name="operation-result", namespace="http://workflow.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowOperationResultImpl implements WorkflowOperationResult {
  @XmlElement(name="mediapackage")
  protected MediaPackage resultingMediaPackage;
  
  @XmlAttribute(name="action")
  protected Action action;

  @XmlAttribute(name="wait")
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
  public WorkflowOperationResultImpl(MediaPackage resultingMediaPackage, Action action) {
    this.resultingMediaPackage = resultingMediaPackage;
    if(action == null) {
      throw new IllegalArgumentException("action must not be null.");
    } else {
      this.action = action;
    }
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationResult#getResultingMediaPackage()
   */
  public MediaPackage getResultingMediaPackage() {
    return resultingMediaPackage;
  }

  /**
   * Sets the resulting media package.
   * @param resultingMediaPackage
   */
  public void setResultingMediaPackage(MediaPackage resultingMediaPackage) {
    this.resultingMediaPackage = resultingMediaPackage;
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
   * Allows JAXB handling of {@link WorkflowOperationResult} interfaces.
   */
  static class Adapter extends XmlAdapter<WorkflowOperationResultImpl, WorkflowOperationResult> {
    public WorkflowOperationResultImpl marshal(WorkflowOperationResult op) throws Exception {return (WorkflowOperationResultImpl)op;}
    public WorkflowOperationResult unmarshal(WorkflowOperationResultImpl op) throws Exception {return op;}
  }

}
