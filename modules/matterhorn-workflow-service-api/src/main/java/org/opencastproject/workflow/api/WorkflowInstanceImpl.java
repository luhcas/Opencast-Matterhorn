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

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

@XmlType(name="workflow", namespace="http://workflow.opencastproject.org/")
@XmlRootElement(name="workflow", namespace="http://workflow.opencastproject.org/")
@XmlAccessorType(XmlAccessType.NONE)
public class WorkflowInstanceImpl implements WorkflowInstance {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowInstanceImpl.class);

  public WorkflowInstanceImpl() {}
  
  public WorkflowInstanceImpl(WorkflowDefinition def, MediaPackage mediaPackage, Long parentWorkflowId, Map<String, String> properties) {
    this.title = def.getTitle();
    this.template = def.getId();
    this.description = def.getDescription();
    this.parentId = parentWorkflowId;
    this.state = WorkflowState.INSTANTIATED;
    this.mediaPackage = mediaPackage;
    this.operations = new ArrayList<WorkflowOperationInstance>();
    List<WorkflowOperationDefinition> operationDefinitions = def.getOperations();
    for(int i = 0; i < operationDefinitions.size(); i++) {
      WorkflowOperationDefinition opDef = operationDefinitions.get(i);
      WorkflowOperationInstanceImpl opInstance = new WorkflowOperationInstanceImpl(opDef);
      opInstance.setPosition(i);
      operations.add(opInstance);
    }
    this.operationsInitialized = true;
    this.configurations = new TreeSet<WorkflowConfiguration>();
    if(properties != null) {
      for(Entry<String, String> entry : properties.entrySet()) {
        configurations.add(new WorkflowConfigurationImpl(entry.getKey(), entry.getValue()));
      }
    }    
  }
  
  /** whether we have initialized the operation positions */
  protected boolean operationsInitialized = false;

  @XmlAttribute()
  private long id;
  
  @XmlAttribute()
  private WorkflowState state;

  @XmlElement(name="template")
  private String template;

  @XmlElement(name="title")
  private String title;
  
  @XmlElement(name="description")
  private String description;

  @XmlElement(name="parent", nillable=true)
  private Long parentId;
  
  @XmlElement(name="mediapackage")
  private MediaPackage mediaPackage;
  
  @XmlElement(name="operation")
  @XmlElementWrapper(name="operations")
  protected List<WorkflowOperationInstance> operations;
  
  @XmlElement(name="configuration")
  @XmlElementWrapper(name="configurations")
  protected Set<WorkflowConfiguration> configurations;

  @XmlElement(name="error")
  @XmlElementWrapper(name="errors")
  protected String[] errorMessages = new String[0];
  
  protected WorkflowOperationInstance currentOperation = null;

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getId()
   */
  public long getId() {
    return id;
  }

  /**
   * Sets the identifier of this workflow instance
   * 
   * @param id
   */
  public void setId(long id) {
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
   * @return the parentId
   */
  public Long getParentId() {
    return parentId;
  }

  /**
   * @param parentId the parentId to set
   */
  public void setParentId(Long parentId) {
    this.parentId = parentId;
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
    return new ArrayList<WorkflowOperationInstance>(operations);
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
   * @see org.opencastproject.workflow.api.WorkflowInstance#setMediaPackage(org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public void setMediaPackage(MediaPackage mediaPackage) {
    this.mediaPackage = mediaPackage;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.Configurable#getConfiguration(java.lang.String)
   */
  @Override
  public String getConfiguration(String key) {
    if(key == null || configurations == null) return null;
    for(WorkflowConfiguration config : configurations) {
      if(config.getKey().equals(key)) return config.getValue();
    }
    return null;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.Configurable#getConfigurationKeys()
   */
  @Override
  public Set<String> getConfigurationKeys() {
    Set<String> keys = new TreeSet<String>();
    if(configurations != null && ! configurations.isEmpty()) {
      for(WorkflowConfiguration config : configurations) {
        keys.add(config.getKey());
      }
    }
    return keys;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.Configurable#removeConfiguration(java.lang.String)
   */
  @Override
  public void removeConfiguration(String key) {
    if(key == null || configurations == null) return;
    for(Iterator<WorkflowConfiguration> configIter = configurations.iterator(); configIter.hasNext();) {
      WorkflowConfiguration config = configIter.next();
      if(config.getKey().equals(key)) {
        configIter.remove();
        return;
      }
    }
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.Configurable#setConfiguration(java.lang.String, java.lang.String)
   */
  @Override
  public void setConfiguration(String key, String value) {
    if(key == null || configurations == null) return;
    for(WorkflowConfiguration config : configurations) {
      if(config.getKey().equals(key)) {
        ((WorkflowConfigurationImpl)config).setValue(value);
        return;
      }
    }
    // No configurations were found, so add a new one
    configurations.add(new WorkflowConfigurationImpl(key, value));
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
    return "Workflow {" + id + "}";
  }

  /**
   * {@inheritDoc}
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return (int)(id >> 32);
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
    if (id != other.id)
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
      for(int i = 0; i < operations.size(); i++) {
        ((WorkflowOperationInstanceImpl)operations.get(i)).setPosition(i);
      }
    }
    logger.debug("workflow instance initialized with operation={}", currentOperation);
  }

  /**
   * @return the template
   */
  public String getTemplate() {
    return template;
  }

  /**
   * @param template the template to set
   */
  public void setTemplate(String template) {
    this.template = template;
  }

  /**
   * @return the errorMessages
   */
  public String[] getErrorMessages() {
    return errorMessages;
  }

  /**
   * @param errorMessages the errorMessages to set
   */
  public void setErrorMessages(String[] errorMessages) {
    this.errorMessages = errorMessages;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#addErrorMessage(java.lang.String)
   */
  @Override
  public void addErrorMessage(String localizedMessage) {
    String[] errors = Arrays.copyOf(this.errorMessages, this.errorMessages.length + 1);
    errors[errors.length-1] = localizedMessage;
    this.errorMessages = errors;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#extend(org.opencastproject.workflow.api.WorkflowDefinition)
   */
  @Override
  public void extend(WorkflowDefinition workflowDefinition) {
    for (WorkflowOperationDefinition operationDefintion : workflowDefinition.getOperations()) {
      this.operations.add(new WorkflowOperationInstanceImpl(operationDefintion));
    }
  }
  
}
