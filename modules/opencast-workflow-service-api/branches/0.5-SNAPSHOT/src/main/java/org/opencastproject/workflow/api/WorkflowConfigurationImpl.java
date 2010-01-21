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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * JAXB annotated implementation of {@link WorkflowConfiguration}
 */
@XmlType(name="configuration", namespace="http://workflow.opencastproject.org/")
@XmlRootElement(name="configuration", namespace="http://workflow.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowConfigurationImpl implements WorkflowConfiguration {
  @XmlAttribute
  protected String key;
  @XmlValue
  protected String value;

  public WorkflowConfigurationImpl() {}
  public WorkflowConfigurationImpl(String key, String value) {
    this.key = key;
    this.value = value;
  }
  
  public String getKey() {
    return key;
  }
  public void setKey(String key) {
    this.key = key;
  }
  public String getValue() {
    return value;
  }
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * Allows JAXB handling of {@link WorkflowConfiguration} interfaces.
   */
  static class Adapter extends XmlAdapter<WorkflowConfigurationImpl, WorkflowConfiguration> {
    public WorkflowConfigurationImpl marshal(WorkflowConfiguration config) throws Exception {return (WorkflowConfigurationImpl) config;}
    public WorkflowConfiguration unmarshal(WorkflowConfigurationImpl config) throws Exception {return config;}
  }
}
