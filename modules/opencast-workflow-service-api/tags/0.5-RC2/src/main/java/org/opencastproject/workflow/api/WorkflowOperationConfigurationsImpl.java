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

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * JAXB annotated implementation of {@link WorkflowOperationConfigurations}
 */
@XmlType(name="scope", namespace="http://workflow.opencastproject.org/")
@XmlRootElement(name="scope", namespace="http://workflow.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowOperationConfigurationsImpl implements WorkflowOperationConfigurations {

  @XmlAttribute
  protected String name;
  
  @XmlElement(name="configuration")
  protected Set<WorkflowConfiguration> configurations;
  
  public WorkflowOperationConfigurationsImpl() {}
  
  public WorkflowOperationConfigurationsImpl(String name, Set<WorkflowConfiguration> configurations){
    this.name = name;
    this.configurations = configurations;
  }
  
  public String getName() {
    return name;
  }

  public void setName(String name){
    this.name = name;
  }
  
  public Set<WorkflowConfiguration> getConfigurations() {
    return configurations;
  }
  
  public void setConfigurations(Set<WorkflowConfiguration> configurations){
    this.configurations = configurations;
  }

  /**
   * Allows JAXB handling of {@link WorkflowOperationConfigurations} interfaces.
   */
  static class Adapter extends XmlAdapter<WorkflowOperationConfigurationsImpl, WorkflowOperationConfigurations>{
    public WorkflowOperationConfigurationsImpl marshal(WorkflowOperationConfigurations config) throws Exception {return (WorkflowOperationConfigurationsImpl) config; }
    public WorkflowOperationConfigurations unmarshal(WorkflowOperationConfigurationsImpl config) throws Exception {return config;}
  }
}
