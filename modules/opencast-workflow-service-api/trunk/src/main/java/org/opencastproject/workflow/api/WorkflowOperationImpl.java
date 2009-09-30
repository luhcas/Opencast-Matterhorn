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
import javax.xml.bind.annotation.adapters.XmlAdapter;


/**
 * See {@link WorkflowOperation}
 */
@XmlType(name="operation", namespace="http://workflow.opencastproject.org/")
@XmlRootElement(name="operation", namespace="http://workflow.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowOperationImpl implements WorkflowOperation {

  @XmlAttribute(name="name")
  protected String name;

  @XmlAttribute(name="description")
  protected String description;

  @XmlAttribute(name="fail-on-error")
  protected boolean failOnError;

  /** A no-arg constructor is needed by JAXB */
  public WorkflowOperationImpl() {}
  
  /**
   * @param name The unique name of this operation
   * @param description The description of what this operation does
   * @param failOnError Whether an exception thrown by this operation should fail the entire {@link WorkflowInstance}
   */
  public WorkflowOperationImpl(String name, String description, boolean failOnError) {
    super();
    this.name = name;
    this.description = description;
    this.failOnError = failOnError;
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
  public boolean isFailOnError() {
    return failOnError;
  }
  public void setFailOnError(boolean failOnError) {
    this.failOnError = failOnError;
  }
  
  static class Adapter extends XmlAdapter<WorkflowOperationImpl, WorkflowOperation> {
    public WorkflowOperationImpl marshal(WorkflowOperation op) throws Exception {return (WorkflowOperationImpl)op;}
    public WorkflowOperation unmarshal(WorkflowOperationImpl op) throws Exception {return op;}
  }
}
