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
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.jaxb.MediapackageType;

import org.apache.commons.io.IOUtils;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * A JAXB-annotated implementation of {@link WorkflowOperationResult}
 */
@XmlType(name="operation-result", namespace="http://workflow.opencastproject.org/")
@XmlRootElement(name="operation-result", namespace="http://workflow.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowOperationResultImpl implements WorkflowOperationResult {
  @XmlElement(name="mediapackage")
  protected MediapackageType resultingMediaPackage;
  
  @XmlElementWrapper(name="properties")
  protected HashMap<String, String> resultingProperties;

  @XmlAttribute(name="wait")
  protected boolean wait;

  public WorkflowOperationResultImpl() {}
  
  public WorkflowOperationResultImpl(MediapackageType resultingMediaPackage, HashMap<String, String> resultingProperties, boolean wait) {
    this.resultingMediaPackage = resultingMediaPackage;
    this.resultingProperties = resultingProperties;
    this.wait = wait;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationResult#getResultingMediaPackage()
   */
  public MediaPackage getResultingMediaPackage() {
    try {
      MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
      return builder.loadFromManifest(IOUtils.toInputStream(resultingMediaPackage.toXml()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationResult#getResultingProperties()
   */
  public Map<String, String> getResultingProperties() {
    return resultingProperties;
  }
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationResult#isWait()
   */
  public boolean isWait() {
    return wait;
  }
  public void setResultingMediaPackage(MediapackageType resultingMediaPackage) {
    this.resultingMediaPackage = resultingMediaPackage;
  }
  public void setResultingProperties(HashMap<String, String> resultingProperties) {
    this.resultingProperties = resultingProperties;
  }
  public void setWait(boolean wait) {
    this.wait = wait;
  }

  /**
   * Allows JAXB handling of {@link WorkflowOperationResult} interfaces.
   */
  static class Adapter extends XmlAdapter<WorkflowOperationResultImpl, WorkflowOperationResult> {
    public WorkflowOperationResultImpl marshal(WorkflowOperationResult op) throws Exception {return (WorkflowOperationResultImpl)op;}
    public WorkflowOperationResult unmarshal(WorkflowOperationResultImpl op) throws Exception {return op;}
  }

}
