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
import org.opencastproject.workflow.api.WorkflowInstance.State;

import java.util.Iterator;
import java.util.Set;

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
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowOperationInstanceImpl implements WorkflowOperationInstance {
  
  @XmlAttribute(name="name")
  protected String name;

  @XmlAttribute(name="state")
  protected String state;

  @XmlAttribute(name="description")
  protected String description;

  @XmlElement(name="result")
  protected WorkflowOperationResult result;
  
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
    this.name = def.getName();
    this.state = State.RUNNING.name();
    this.description = def.getDescription();
    this.configurations = def.getConfigurations();
  }
  
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

//  /**
//   * {@inheritDoc}
//   * @see org.opencastproject.workflow.api.WorkflowOperationInstance#getResult()
//   */
//  public MediaPackage getResult() {
//    try {
//      return MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromManifest(IOUtils.toInputStream(mediaPackageType.toXml()));
//    } catch (Exception e) {
//      throw new RuntimeException(e);
//    }
//  }
  
//  public void setResult(MediaPackage mediaPackage) {
//    try {
//      this.mediaPackageType = MediapackageType.fromXml(mediaPackage.toXml());
//    } catch (Exception e) {
//      throw new RuntimeException(e);
//    }
//  }

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
  public State getState() {
    return State.valueOf(state);
  }
  
  public void setState(String state) {
    this.state = state;
  }

  public void setResult(WorkflowOperationResult result) {
    this.result = result;
  }

  public WorkflowOperationResult getResult() {
    return result;
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

}
