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
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

@Entity(name="workflow")
@Table(name="MH_WORKFLOW")
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
    this.configurations = new TreeSet<WorkflowConfigurationImpl>();
    if(properties != null) {
      for(Entry<String, String> prop : properties.entrySet()) {
        configurations.add(new WorkflowConfigurationImpl(prop.getKey(), prop.getValue()));
      }
    }
    this.operations = new ArrayList<WorkflowOperationInstanceImpl>();
    for(WorkflowOperationDefinition opDef : def.getOperations()) {
      operations.add(new WorkflowOperationInstanceImpl(opDef));
    }
  }

  @Id
  @XmlID
  @XmlAttribute()
  protected String id;

  @Column(name="state")
  @XmlAttribute()
  protected WorkflowState state;

  @Column(name="template")
  @XmlElement(name="template")
  protected String title;

  @Column(name="description")
  @XmlElement(name="description")
  protected String description;

  @ElementCollection
  @CollectionTable(name="WF_CONFIG")
  @XmlElement(name="configuration")
  @XmlElementWrapper(name="configurations")
  protected Set<WorkflowConfigurationImpl> configurations;
  
  @Transient
  @XmlElement(name="mediapackage")
  protected MediaPackage mediaPackage;

  @ElementCollection
  @CollectionTable(name="WF_OP")
  @XmlElement(name="operation")
  @XmlElementWrapper(name="operations")
  protected List<WorkflowOperationInstanceImpl> operations;
  
  @JoinTable(name="WF_OP")
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
    if(operations == null) operations = new ArrayList<WorkflowOperationInstanceImpl>();
    return new ArrayList<WorkflowOperationInstance>(operations);
  }

  /**
   * Sets the workflow operations on this workflow instance
   * 
   * @param workflowOperationInstanceList
   */
  public void setOperations(List<WorkflowOperationInstance> workflowOperationInstanceList) {
    if(operations == null) {
      operations = new ArrayList<WorkflowOperationInstanceImpl>();
    } else {
      operations.clear();
    }
    if(workflowOperationInstanceList != null) {
      for(WorkflowOperationInstance op : workflowOperationInstanceList) {
        operations.add((WorkflowOperationInstanceImpl)op);
      }
    }
    init(); // If we are resetting the operations, we need to update the current operation
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
   * @see org.opencastproject.workflow.api.WorkflowInstance#getConfiguration(java.lang.String)
   */ 
  public String getConfiguration(String key) {
    if(key == null || configurations == null) return null;
    for(WorkflowConfiguration config : configurations) {
      if(key.equals(config.getKey())) return config.getValue();
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#removeConfiguration(java.lang.String)
   */
  public void removeConfiguration(String key) {
    if(key == null || configurations == null) return;
    for(Iterator<WorkflowConfigurationImpl> configIter = configurations.iterator(); configIter.hasNext();) {
      if(key.equals(configIter.next().getKey())) configIter.remove();
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#setConfiguration(java.lang.String, java.lang.String)
   */
  public void setConfiguration(String key, String value) {
    if(configurations == null) configurations = new TreeSet<WorkflowConfigurationImpl>();
    configurations.add(new WorkflowConfigurationImpl(key, value));
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.Configurable#getConfigurationKeys()
   */
  @Override
  public Set<String> getConfigurationKeys() {
    Set<String> set = new TreeSet<String>();
    for(WorkflowConfiguration config : configurations) {
      set.add(config.getKey());
    }
    return set;
  }

  /**
   * @return the configurations
   */
  public Set<WorkflowConfiguration> getConfigurations() {
    return new TreeSet<WorkflowConfiguration>(configurations);
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
    for(Iterator<WorkflowOperationInstanceImpl> opIter = operations.iterator(); opIter.hasNext();) {
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
  void init() {
    if(operations != null) {
      for(WorkflowOperationInstance operation : operations) {
        if(OperationState.RUNNING.equals(operation.getState()) || OperationState.PAUSED.equals(operation.getState())) {
          this.currentOperation = operation;
          break;
        }
      }
    }
  }

  /**
   * Gets an xml representation of the current mediapackage to store in the workflow DB.
   */
  @Lob
  @Basic(fetch=FetchType.EAGER)
  public String getMediaPackageAsXml() {
    try {
      return mediaPackage == null ? null : mediaPackage.toXml();
    } catch(MediaPackageException e) {
      logger.warn("unable to marshall mediapackage {}", mediaPackage);
      throw new RuntimeException(e);
    }
  }
  public void setMediaPackageAsXml(String mediaPackageAsXml) {
    if(mediaPackageAsXml != null) {
      try {
        this.mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(mediaPackageAsXml);
      } catch(MediaPackageException e) {
        logger.warn("unable to unmarshall mediapackage {}", mediaPackageAsXml);
        throw new RuntimeException(e);
      }
    }
  }

}
