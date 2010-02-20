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

import java.util.Iterator;
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

/**
 * A JAXB-annotated implementation of {@link WorkflowOperationInstance}
 */
@XmlType(name="operation-instance", namespace="http://workflow.opencastproject.org/")
@XmlRootElement(name="operation-instance", namespace="http://workflow.opencastproject.org/")
@XmlAccessorType(XmlAccessType.NONE)
public class WorkflowOperationInstanceImpl implements WorkflowOperationInstance {

  @XmlAttribute(name="id")
  protected String id;

  @XmlAttribute(name="state")
  protected OperationState state;

  @XmlAttribute(name="description")
  protected String description;
  
  @XmlElement(name="configuration")
  @XmlElementWrapper(name="configurations")
  protected Set<WorkflowConfiguration> configurations;

  /**
   * No-arg constructor needed for JAXB serialization
   */
  public WorkflowOperationInstanceImpl() {}

  /**
   * Builds a new workflow operation instance based on another workflow operation.
   */
  public WorkflowOperationInstanceImpl(WorkflowOperationDefinition def) {
    this.id = def.getId();
    this.state = OperationState.INSTANTIATED;
    this.description = def.getDescription();
    Set<String> defConfigs = def.getConfigurationKeys();
    this.configurations = new TreeSet<WorkflowConfiguration>();
    if(defConfigs != null) {
      for(String key : defConfigs) {
        configurations.add(new WorkflowConfigurationImpl(key, def.getConfiguration(key)));
      }
    }
  }
  
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  static class Adapter extends XmlAdapter<WorkflowOperationInstanceImpl, WorkflowOperationInstance> {
    public WorkflowOperationInstanceImpl marshal(WorkflowOperationInstance op) throws Exception {return (WorkflowOperationInstanceImpl)op;}
    public WorkflowOperationInstance unmarshal(WorkflowOperationInstanceImpl op) throws Exception {return op;}
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationInstance#getOutput()
   */
  public MediaPackage getOutput() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationInstance#getState()
   */
  public OperationState getState() {
    return state;
  }
  
  public void setState(OperationState state) {
    this.state = state;
  }

  public Set<WorkflowConfiguration> getConfigurations() {
    return configurations;
  }

  public void setConfiguration(Set<WorkflowConfiguration> configurations) {
    this.configurations = configurations;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getConfiguration(java.lang.String)
   */
  public String getConfiguration(String key) {
    if(key == null || configurations == null) return null;
    for(WorkflowConfiguration config : configurations) {
      if(config.getKey().equals(key)) return config.getValue();
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#removeConfiguration(java.lang.String)
   */
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
   * @see org.opencastproject.workflow.api.WorkflowInstance#setConfiguration(java.lang.String, java.lang.String)
   */
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
   * @see org.opencastproject.workflow.api.WorkflowOperationInstance#getConfigurationKeys()
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
}
