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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

@XmlType(name="workflow-instance", namespace="http://workflow.opencastproject.org/")
@XmlRootElement(name="workflow-instance", namespace="http://workflow.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowInstanceImpl implements WorkflowInstance {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowInstanceImpl.class);

  public WorkflowInstanceImpl() {}
  
  public WorkflowInstanceImpl(WorkflowDefinition def) {
    this.title = def.getTitle();
    this.description = def.getDescription();
    this.workflowOperationDefinitionList = def.getOperations();
  }

  @XmlID
  @XmlAttribute()
  private String id;
  
  @XmlAttribute()
  private String state;

  @XmlElement(name="title")
  private String title;

  @XmlElement(name="description")
  private String description;

  @XmlElement(name="scope")
  @XmlElementWrapper(name="configurations")
  protected Set<WorkflowOperationConfigurations> configurations;
  
  @XmlElement(name="mediapackage")
  private MediaPackage sourceMediaPackage;
  
  @XmlElement(name="operation-definitions")
  private WorkflowOperationDefinitionList workflowOperationDefinitionList;

  @XmlElement(name="operation-instances")
  public WorkflowOperationInstanceList workflowOperationInstanceList;

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

  public State getState() {
    return State.valueOf(state);
  }
  public void setState(String state) {
    this.state = state;
  }

  public MediaPackage getSourceMediaPackage() {
    return sourceMediaPackage;
  }
  public void setSourceMediaPackage(MediaPackage sourceMediaPackage) {
    this.sourceMediaPackage = sourceMediaPackage;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getCurrentOperation()
   */
  public WorkflowOperationInstance getCurrentOperation() {
    // Since we add operation instances only once they start, we can just return the last one in the list.
    if(workflowOperationInstanceList == null) return null;
    List<WorkflowOperationInstance> list = workflowOperationInstanceList.getOperationInstance();
    if(list.isEmpty()) return null;
    return list.get(list.size()-1);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getWorkflowOperations()
   */
  public WorkflowOperationInstanceList getWorkflowOperationInstanceList() {
    if(workflowOperationInstanceList == null) workflowOperationInstanceList = new WorkflowOperationInstanceListImpl();
    return workflowOperationInstanceList;
  }

  public void setWorkflowOperationInstanceList(WorkflowOperationInstanceList workflowOperationInstanceList) {
    this.workflowOperationInstanceList = workflowOperationInstanceList;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#addWorkflowOperationDefinition(org.opencastproject.workflow.api.WorkflowOperationDefinition)
   */
  public void addWorkflowOperationDefinition(WorkflowOperationDefinition operation) {
    // TODO Auto-generated method stub
    
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#addWorkflowOperationDefinition(int, org.opencastproject.workflow.api.WorkflowOperationDefinition)
   */
  public void addWorkflowOperationDefinition(int location, WorkflowOperationDefinition operation) {
    // TODO Auto-generated method stub
    
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#addWorkflowOperationDefinitions(org.opencastproject.workflow.api.WorkflowOperationDefinitionList)
   */
  public void addWorkflowOperationDefinitions(WorkflowOperationDefinitionList operations) {
    getWorkflowOperationDefinitionList().getOperation().addAll(operations.getOperation());
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#addWorkflowOperationDefinitions(int, org.opencastproject.workflow.api.WorkflowOperationDefinitionList)
   */
  public void addWorkflowOperationDefinitions(int location, WorkflowOperationDefinitionList operations) {
    getWorkflowOperationDefinitionList().getOperation().addAll(location, operations.getOperation());
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getWorkflowOperationDefinitionList()
   */
  public WorkflowOperationDefinitionList getWorkflowOperationDefinitionList() {
    return workflowOperationDefinitionList;
  }
  
  public void setWorkflowOperationDefinitionList(WorkflowOperationDefinitionList workflowOperationDefinitionList) {
    this.workflowOperationDefinitionList = workflowOperationDefinitionList;
  }

  public String toString() {
    return new ToStringBuilder("workflow instance").append(this.id).append(this.title).toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

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
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getCurrentMediaPackage()
   */
  public MediaPackage getCurrentMediaPackage() {
    if(workflowOperationInstanceList == null || workflowOperationInstanceList.size() == 0) return getSourceMediaPackage();
    WorkflowOperationInstance op = workflowOperationInstanceList.get(workflowOperationInstanceList.size()-1);
    WorkflowOperationResult result = op.getResult();
    if(result == null || result.getResultingMediaPackage() == null) {
      // this could be the currently running operation.  if so, try to get the previous operation's result
      if(op.getState().equals(State.RUNNING) && workflowOperationInstanceList.size() >= 2) {
        WorkflowOperationInstance previous = workflowOperationInstanceList.get(workflowOperationInstanceList.size()-2);
        WorkflowOperationResult previousResult =  previous.getResult();
        if(previousResult != null) return previousResult.getResultingMediaPackage();
      } else {
        return getSourceMediaPackage();
      }
    }
    return (result != null) ? result.getResultingMediaPackage() : getSourceMediaPackage();
  }

  /**
   * {@inheritDoc} Global configurations are stored under the scope 'global'.
   * @see org.opencastproject.workflow.api.WorkflowInstance#getConfigurations()
   */
  public Set<WorkflowConfiguration> getConfigurations() {
    if(configurations != null && configurations.size() > 0) {
      for(WorkflowOperationConfigurations confs : configurations){
        if("global".equals(confs.getName()) && confs.getConfigurations() != null){
          return confs.getConfigurations();
        }
      }
    }
    return new HashSet<WorkflowConfiguration>();
  }

  public void setConfigurations(Set<WorkflowOperationConfigurations> configurations) {
    this.configurations = configurations;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getLocalConfigurations(java.lang.String)
   */
  public Set<WorkflowConfiguration> getLocalConfigurations(String operation){
    for(WorkflowOperationConfigurations confs : configurations){
      if(confs.getName().equals(operation)){
        return confs.getConfigurations();
      }
    }
    return new HashSet<WorkflowConfiguration>();
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getConfiguration(java.lang.String)
   */ 
  public String getConfiguration(String key) {
    if(key == null || configurations == null) return null;
    String[] result = scopeDeterminer(key);
    String scope = result[0];
    String newKey = result[1];
    for(WorkflowOperationConfigurations confs : configurations){
      if(confs.getName().equals(scope)){
        // there is configuration in specified scope (or global if scope was not specified)
        for(WorkflowConfiguration config : confs.getConfigurations()) {
          if(config.getKey().equals(newKey)) return config.getValue();
        }
        return null;
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#removeConfiguration(java.lang.String)
   */
  public void removeConfiguration(String key) {
    if(key == null || configurations == null) return;
    String[] result = scopeDeterminer(key);
    String scope = result[0];
    String newKey = result[1];
    for(WorkflowOperationConfigurations confs : configurations){
      if(confs.getName().equals(scope)){
        for(Iterator<WorkflowConfiguration> configIter = confs.getConfigurations().iterator(); configIter.hasNext();) {
          WorkflowConfiguration config = configIter.next();
          // remove configuration in specified scope
          if(config.getKey().equals(newKey)) {
            configIter.remove();
            return;
          }
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#setConfiguration(java.lang.String, java.lang.String)
   */
  public void setConfiguration(String key, String value) {
    if(key == null || configurations == null) return;
    String[] result = scopeDeterminer(key);
    String scope = result[0];
    String newKey = result[1];
    for(WorkflowOperationConfigurations confs : configurations){
      if(confs.getName().equals(scope)){
        for(WorkflowConfiguration config : confs.getConfigurations()) {
          if(config.getKey().equals(newKey)) {
            ((WorkflowConfigurationImpl)config).setValue(value);
            return;
          }
        }
        // No configurations were found, so add a new one
        confs.getConfigurations().add(new WorkflowConfigurationImpl(newKey, value));
        return;
      }
    }
    //configurations.add(new WorkflowConfigurationImpl(key, value));
  }
  
  /**
   * The helper method which determines in which scope does configuration belong based to the prefix of key.
   * @param key
   *        
   * @return scope and new key
   */
  protected String[] scopeDeterminer(String key){
    if(key.contains("/")){
      return key.split("/", 2);
    } else {
      return new String[]{"global", key};
    }
  }
}
