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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * A JAXB-annotated implementation of {@link WorkflowOperationInstance}
 */
@XmlType(name="operation-instance", namespace="http://workflow.opencastproject.org/")
@XmlRootElement(name="operation-instance", namespace="http://workflow.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowOperationInstanceImpl extends WorkflowOperationDefinitionImpl implements WorkflowOperationInstance {
  @XmlElement(name="mediapackage")
  protected MediapackageType mediaPackageType;
  
  /**
   * No-arg constructor needed for JAXB serialization
   */
  public WorkflowOperationInstanceImpl() {}

  /**
   * Builds a new workflow operation instance based on a workflow operation definition
   * @param def The workflow operation definition
   */
  public WorkflowOperationInstanceImpl(WorkflowOperationDefinition def) {
    this.name = def.getName();
    this.description = def.getDescription();
    this.failOnError = def.isFailOnError();
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationInstance#getResult()
   */
  public MediaPackage getResult() {
    try {
      return MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromManifest(IOUtils.toInputStream(mediaPackageType.toXml()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public void setResult(MediaPackage mediaPackage) {
    try {
      this.mediaPackageType = MediapackageType.fromXml(mediaPackage.toXml());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  static class Adapter extends XmlAdapter<WorkflowOperationInstanceImpl, WorkflowOperationInstance> {
    public WorkflowOperationInstanceImpl marshal(WorkflowOperationInstance op) throws Exception {return (WorkflowOperationInstanceImpl)op;}
    public WorkflowOperationInstance unmarshal(WorkflowOperationInstanceImpl op) throws Exception {return op;}
  }

}
