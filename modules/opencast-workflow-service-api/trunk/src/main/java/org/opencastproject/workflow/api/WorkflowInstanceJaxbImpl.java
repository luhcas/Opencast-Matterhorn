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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "workflow-instance", namespace = "http://workflow.opencastproject.org/")
@XmlRootElement(name = "workflow-instance", namespace = "http://workflow.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowInstanceJaxbImpl implements WorkflowInstance {

  private static final Logger logger = LoggerFactory.getLogger(WorkflowInstanceJaxbImpl.class);

  public WorkflowInstanceJaxbImpl() {
  }

  @XmlID
  @XmlAttribute()
  private String id;

  @XmlAttribute()
  private String state;

  @XmlElement(name = "workflow-definition")
  private WorkflowDefinitionJaxbImpl workflowDefinition;

  @XmlElement(name = "title")
  private String title;

  @XmlElement(name = "description")
  private String description;

  @XmlElement(name = "mediapackage")
  private MediapackageType mediaPackageType;

  @XmlElementWrapper(name = "properties")
  private HashMap<String, String> properties;

  @XmlElementWrapper(name = "operations")
  private List<WorkflowOperation> operations;

  @XmlElement(name = "current-operation")
  private WorkflowOperation currentOperation;

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

  public void setWorkflowDefinition(WorkflowDefinitionJaxbImpl workflowDefinition) {
    this.workflowDefinition = workflowDefinition;
    this.operations = new ArrayList<WorkflowOperation>();
    this.operations.addAll(workflowDefinition.getOperations());
  }

  public State getState() {
    return State.valueOf(state);
  }

  public void setState(String state) {
    this.state = state;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowInstance#getCurrentOperation()
   */
  public WorkflowOperation getCurrentOperation() {
    return currentOperation;
  }

  /**
   * Sets the current workflow operation.
   * 
   * @param operation
   *          the current operation
   */
  public void setCurrentOperation(WorkflowOperation operation) {
    this.currentOperation = operation;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowInstance#getWorkflowOperations()
   */
  public WorkflowOperation[] getWorkflowOperations() {
    return operations.toArray(new WorkflowOperation[operations.size()]);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowInstance#addWorkflowOperation(org.opencastproject.workflow.api.WorkflowOperation)
   */
  public void addWorkflowOperation(WorkflowOperation operation) {
    operations.add(operation);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowInstance#addWorkflowOperation(int,
   *      org.opencastproject.workflow.api.WorkflowOperation)
   */
  public void addWorkflowOperation(int location, WorkflowOperation operation) {
    operations.add(location, operation);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowInstance#addWorkflowOperations(java.util.List)
   */
  public void addWorkflowOperations(List<WorkflowOperation> operations) {
    operations.addAll(operations);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowInstance#addWorkflowOperations(int, java.util.List)
   */
  public void addWorkflowOperations(int location, List<WorkflowOperation> operations) {
    operations.addAll(location, operations);
  }

  public MediapackageType getMediaPackageType() {
    return mediaPackageType;
  }

  public void setMediaPackageType(MediapackageType mediaPackageType) {
    this.mediaPackageType = mediaPackageType;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowInstance#getProperties()
   */
  public HashMap<String, String> getProperties() {
    return properties;
  }
  
  public String getProperty(String name) {
    return properties.get(name);
  }

  /**
   * Sets the properties forming the environment for this workflow instance.
   * 
   * @param properties
   *          the initial set of properties
   */
  public void setProperties(HashMap<String, String> properties) {
    this.properties = properties;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#setProperty(java.lang.String, java.lang.String)
   */
  public void setProperty(String name, String value) {
    properties.put(name, value);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#removeProperty(java.lang.String)
   */
  public void removeProperty(String name) {
    properties.remove(name);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowInstance#getMediaPackage()
   */
  public MediaPackage getMediaPackage() {
    if (mediaPackageType == null)
      return null;
    try {
      return MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromManifest(
              IOUtils.toInputStream(mediaPackageType.toXml()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
