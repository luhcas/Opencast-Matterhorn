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
import org.opencastproject.media.mediapackage.jaxb.MediapackageType;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name="workflow-instance", namespace="http://workflow.opencastproject.org/")
@XmlRootElement(name="workflow-instance", namespace="http://workflow.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowInstanceImpl implements WorkflowInstance {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowInstanceImpl.class);

  public WorkflowInstanceImpl() {}

  @XmlID
  @XmlAttribute()
  private String id;
  
  @XmlAttribute()
  private String state;

  @XmlElement(name="workflow-definition")
  private WorkflowDefinitionImpl workflowDefinition;

  @XmlElement(name="title")
  private String title;

  @XmlElement(name="description")
  private String description;

  @XmlElement(name="mediapackage")
  private MediapackageType sourceMediaPackageType;
  
  @XmlElementWrapper(name="properties")
  private HashMap<String, String> properties;

  @XmlElement(name="operation-instances")
  public WorkflowOperationInstanceList workflowOperations;

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

  public WorkflowDefinition getWorkflowDefinition() {
    return workflowDefinition;
  }

  public void setWorkflowDefinition(WorkflowDefinitionImpl workflowDefinition) {
    this.workflowDefinition = workflowDefinition;
  }
  public State getState() {
    return State.valueOf(state);
  }
  public void setState(String state) {
    this.state = state;
  }

  public MediapackageType getSourceMediaPackageType() {
    return sourceMediaPackageType;
  }
  public void setSourceMediaPackageType(MediapackageType sourceMediaPackageType) {
    this.sourceMediaPackageType = sourceMediaPackageType;
  }
  
  public HashMap<String, String> getProperties() {
    return properties;
  }
  public void setProperties(HashMap<String, String> properties) {
    this.properties = properties;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getSourceMediaPackage()
   */
  public MediaPackage getSourceMediaPackage() {
    if(sourceMediaPackageType == null) return null;
    try {
      return MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromManifest(IOUtils.toInputStream(sourceMediaPackageType.toXml()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#addWorkflowOperation(org.opencastproject.workflow.api.WorkflowOperationInstance)
   */
  public void addWorkflowOperation(WorkflowOperationInstance operation) {
    getWorkflowOperations().getOperationInstance().add(operation);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#addWorkflowOperation(int, org.opencastproject.workflow.api.WorkflowOperationInstance)
   */
  public void addWorkflowOperation(int location, WorkflowOperationInstance operation) {
    getWorkflowOperations().getOperationInstance().add(location, operation);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#addWorkflowOperations(org.opencastproject.workflow.api.WorkflowOperationInstanceList)
   */
  public void addWorkflowOperations(WorkflowOperationInstanceList operations) {
    getWorkflowOperations().getOperationInstance().addAll(operations.getOperationInstance());
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#addWorkflowOperations(int, org.opencastproject.workflow.api.WorkflowOperationInstanceList)
   */
  public void addWorkflowOperations(int location, WorkflowOperationInstanceList operations) {
    getWorkflowOperations().getOperationInstance().addAll(location, operations.getOperationInstance());
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getCurrentOperation()
   */
  public WorkflowOperationInstance getCurrentOperation() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getProperty(java.lang.String)
   */
  public String getProperty(String name) {
    return properties.get(name);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#removeProperty(java.lang.String)
   */
  public void removeProperty(String name) {
    // TODO Auto-generated method stub
    
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#setProperty(java.lang.String, java.lang.String)
   */
  public void setProperty(String name, String value) {
    // TODO Auto-generated method stub
    
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getWorkflowOperations()
   */
  public WorkflowOperationInstanceList getWorkflowOperations() {
    if(workflowOperations == null) workflowOperations = new WorkflowOperationInstanceListImpl();
    return workflowOperations;
  }

  public void setWorkflowOperations(WorkflowOperationInstanceList workflowOperations) {
    this.workflowOperations = workflowOperations;
  }

}
