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

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.jaxb.MediapackageType;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.State;
import org.opencastproject.workflow.impl.WorkflowInstanceImpl;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * FIXME -- Add javadocs
 */

@XmlType(name="workflow-instance", namespace="http://workflow.opencastproject.org/")
@XmlRootElement(name="workflow-instance", namespace="http://workflow.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowInstanceJaxbImpl {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowInstanceJaxbImpl.class);

  public WorkflowInstanceJaxbImpl() {}
  public WorkflowInstanceJaxbImpl(WorkflowInstance instance) throws Exception {
    logger.info("Creating a " + WorkflowInstanceJaxbImpl.class.getName() + " from " + instance);
    this.id = instance.getId();
    this.title = instance.getTitle();
    this.description = instance.getDescription();
    this.workflowDefinition = new WorkflowDefinitionJaxbImpl(instance.getWorkflowDefinition());
    State stateEnum = instance.getState();
    if(stateEnum != null) this.state = stateEnum.name();
    MediaPackage mp = instance.getMediaPackage();
    if(mp != null) {
      mediaPackage = MediapackageType.fromXml(mp.toXml());
    }
  }

  @XmlTransient
  public WorkflowInstance getEntity() throws Exception {
    WorkflowInstanceImpl entity = new WorkflowInstanceImpl();
    entity.setId(id);
    entity.setTitle(title);
    entity.setDescription(description);
    entity.setWorkflowDefinition(workflowDefinition.getEntity());
    if(state != null) entity.setState(State.valueOf(State.class, this.state));
    if(this.mediaPackage != null) {
      InputStream in = IOUtils.toInputStream(mediaPackage.toXml());
      entity.setMediaPackage(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromManifest(in));
    }
    return entity;
  }

  @XmlID
  @XmlAttribute()
  private String id;
  
  @XmlAttribute()
  private String state;

  @XmlElement(name="workflow-definition")
  private WorkflowDefinitionJaxbImpl workflowDefinition;

  @XmlElement(name="title")
  private String title;

  @XmlElement(name="description")
  private String description;

  @XmlElement(name="mediapackage")
  private MediapackageType mediaPackage;
  
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

  public WorkflowDefinitionJaxbImpl getWorkflowDefinition() {
    return workflowDefinition;
  }

  public void setWorkflowDefinition(WorkflowDefinitionJaxbImpl workflowDefinition) {
    this.workflowDefinition = workflowDefinition;
  }
  public String getState() {
    return state;
  }
  public void setState(String state) {
    this.state = state;
  }
  public MediapackageType getMediaPackage() {
    return mediaPackage;
  }
  public void setMediaPackage(MediapackageType mediaPackage) {
    this.mediaPackage = mediaPackage;
  }

}
