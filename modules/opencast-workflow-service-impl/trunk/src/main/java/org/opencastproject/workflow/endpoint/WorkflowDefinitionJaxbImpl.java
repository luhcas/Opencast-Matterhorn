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
package org.opencastproject.workflow.endpoint;

import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.impl.WorkflowDefinitionImpl;

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
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * FIXME -- Add javadocs
 */

@XmlType(name="workflow-definition", namespace="http://workflow.opencastproject.org/")
@XmlRootElement(name="workflow-definition", namespace="http://workflow.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowDefinitionJaxbImpl {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowDefinitionJaxbImpl.class);

  public WorkflowDefinitionJaxbImpl() {}
  public WorkflowDefinitionJaxbImpl(WorkflowDefinition def) {
    logger.info("Creating a " + WorkflowDefinitionJaxbImpl.class.getName() + " from " + def);
    this.id = def.getId();
    this.title = def.getTitle();
    this.description = def.getDescription();
    this.operations = def.getOperations();
  }

  @XmlTransient
  public WorkflowDefinition getEntity() {
    WorkflowDefinitionImpl entity = new WorkflowDefinitionImpl();
    entity.setId(id);
    entity.setTitle(title);
    entity.setDescription(description);
    entity.setOperations(operations);
    return entity;
  }

  @XmlID
  @XmlAttribute()
  private String id;

  @XmlElement(name="title")
  private String title;

  @XmlElement(name="description")
  private String description;

  @XmlElement(name="operation")
  @XmlElementWrapper(name="operations")
  private List<String> operations;
  
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
  public List<String> getOperations() {
    return operations;
  }
  public void setOperations(List<String> operations) {
    this.operations = operations;
  }

}

