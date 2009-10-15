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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * A JAXB-anotated implementation of {@link WorkflowDefinition}
 */
@XmlType(name="workflow-definition", namespace="http://workflow.opencastproject.org/")
@XmlRootElement(name="workflow-definition", namespace="http://workflow.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowDefinitionImpl implements WorkflowDefinition {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowDefinitionImpl.class);

  public WorkflowDefinitionImpl() {}

  @XmlID
  @XmlAttribute()
  private String id;

  @XmlElement(name="title")
  private String title;

  @XmlElement(name="description")
  private String description;

  @XmlElementWrapper(name="operations")
  private List<WorkflowOperationDefinition> operations;
  
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
  public List<WorkflowOperationDefinition> getOperations() {
    return operations;
  }

  public void setOperations(List<WorkflowOperationDefinition> operations) {
    this.operations = operations;
  }

  /**
   * Since we are posting workflow definitions as one post parameter in a multi-parameter post, we can not rely on
   * "automatic" JAXB deserialization.  We therefore need to provide a static valueOf(String) method to transform an
   * XML string into a WorkflowDefinition.
   * 
   * @param xmlString The xml describing the workflow
   * @return A {@link WorkflowDefinitionImpl} instance based on xmlString
   * @throws Exception If there is a problem marshalling the {@link WorkflowDefinitionImpl} from XML.
   */
  public static WorkflowDefinitionImpl valueOf(String xmlString) throws Exception {
    return (WorkflowDefinitionImpl) WorkflowDefinitionFactory.getInstance().parse(xmlString);
  }
  
  static class Adapter extends XmlAdapter<WorkflowDefinitionImpl, WorkflowDefinition> {
    public WorkflowDefinitionImpl marshal(WorkflowDefinition op) throws Exception {return (WorkflowDefinitionImpl)op;}
    public WorkflowDefinition unmarshal(WorkflowDefinitionImpl op) throws Exception {return op;}
  }

}

