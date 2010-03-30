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
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

@XmlType(name="workflow", namespace="http://workflow.opencastproject.org/")
@XmlRootElement(name="workflow", namespace="http://workflow.opencastproject.org/")
@XmlAccessorType(XmlAccessType.NONE)
public class WorkflowInstanceImpl implements WorkflowInstance {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowInstanceImpl.class);

  public WorkflowInstanceImpl() {}
  
  public WorkflowInstanceImpl(WorkflowDefinition def, MediaPackage mediaPackage, Map<String, String> properties) {
    this.title = def.getId();
    this.description = def.getDescription();
    this.state = WorkflowState.INSTANTIATED;
    this.mediaPackage = mediaPackage;
    this.operations = new ArrayList<WorkflowOperationInstance>();
    for(WorkflowOperationDefinition opDef : def.getOperations()) {
      operations.add(new WorkflowOperationInstanceImpl(opDef));
    }
  }

  @XmlID
  @XmlAttribute()
  private String id;
  
  @XmlAttribute()
  private WorkflowState state;

  @XmlElement(name="template")
  private String title;

  @XmlElement(name="description")
  private String description;

  @XmlElement(name="mediapackage")
  private MediaPackage mediaPackage;
  
  @XmlElement(name="operation")
  @XmlElementWrapper(name="operations")
  protected List<WorkflowOperationInstance> operations;
  
  protected WorkflowOperationInstance currentOperation = null;

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getId()
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the identifier of this workflow instance
   * 
   * @param id
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getTitle()
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the title of this workflow instance
   * @param title
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getDescription()
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description of this workflow instance
   * 
   * @param description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getState()
   */
  public WorkflowState getState() {
    return state;
  }

  /**
   * Sets the state of this workflow instance
   * @param state
   */
  public void setState(WorkflowState state) {
    this.state = state;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getCurrentOperation()
   */
  public WorkflowOperationInstance getCurrentOperation() {
    return currentOperation;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getOperations()
   */
  public List<WorkflowOperationInstance> getOperations() {
    if(operations == null) operations = new ArrayList<WorkflowOperationInstance>();
    return operations;
  }

  /**
   * Sets the workflow operations on this workflow instance
   * 
   * @param workflowOperationInstanceList
   */
  public void setOperations(List<WorkflowOperationInstance> workflowOperationInstanceList) {
    this.operations = workflowOperationInstanceList;
    init();
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getMediaPackage()
   */
  public MediaPackage getMediaPackage() {
    return mediaPackage;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#setMediaPackage(org.opencastproject.media.mediapackage.MediaPackage)
   */
  @Override
  public void setMediaPackage(MediaPackage mediaPackage) {
    this.mediaPackage = mediaPackage;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#next()
   */
  @Override
  public WorkflowOperationInstance next() {
    if(operations == null || operations.size() == 0) throw new IllegalStateException("operations list must contain operations");
    if(currentOperation == null) {
      currentOperation = operations.get(0); 
      return currentOperation;
    }
    for(Iterator<WorkflowOperationInstance> opIter = operations.iterator(); opIter.hasNext();) {
      WorkflowOperationInstance op = opIter.next();
      if(op.equals(currentOperation) && opIter.hasNext()) {
        currentOperation = opIter.next();
        return currentOperation;
      }
    }
    currentOperation = null;
    return null;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#hasNext()
   */
  public boolean hasNext() {
    if(WorkflowState.FAILED.equals(state) || WorkflowState.FAILING.equals(state) || WorkflowState.STOPPED.equals(state) ||
            WorkflowState.SUCCEEDED.equals(state)) return false;
    if(operations == null || operations.size() == 0) throw new IllegalStateException("operations list must contain operations");
    if(currentOperation == null) return true;
    return operations.lastIndexOf(currentOperation) < operations.size()-1;
  }

  /**
   * {@inheritDoc}
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return new ToStringBuilder("workflow instance").append(this.id).append(this.title).toString();
  }

  /**
   * {@inheritDoc}
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  /**
   * {@inheritDoc}
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof WorkflowInstanceImpl))
      return false;
    WorkflowInstanceImpl other = (WorkflowInstanceImpl) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    return true;
  }

  /**
   * Allows JAXB handling of {@link WorkflowInstance} interfaces.
   */
  static class Adapter extends XmlAdapter<WorkflowInstanceImpl, WorkflowInstance> {
    public WorkflowInstanceImpl marshal(WorkflowInstance instance) throws Exception {return (WorkflowInstanceImpl) instance;}
    public WorkflowInstance unmarshal(WorkflowInstanceImpl instance) throws Exception {return instance;}
  }

  /**
   * Initializes the workflow instance
   */
  public void init() {
    if(operations != null) {
      OperationState previousState = null;
      for(WorkflowOperationInstance operation : operations) {
        // If the previous operation succeeded, but this hasn't started yet, this is the current operation
        if(previousState != null && OperationState.SUCCEEDED.equals(previousState) && OperationState.INSTANTIATED.equals(operation.getState())) {
          this.currentOperation = operation;
          break;
        }
        // If an operation is running or paused, this is the current operation
        if(OperationState.RUNNING.equals(operation.getState()) || OperationState.PAUSED.equals(operation.getState())) {
          this.currentOperation = operation;
          break;
        }
        previousState = operation.getState();
      }
    }
  }
}
