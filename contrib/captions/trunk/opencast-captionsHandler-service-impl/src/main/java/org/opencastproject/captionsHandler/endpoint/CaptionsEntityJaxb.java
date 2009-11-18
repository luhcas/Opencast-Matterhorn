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
package org.opencastproject.captionsHandler.endpoint;

import org.opencastproject.captionsHandler.api.CaptionsMediaItem;
import org.opencastproject.captionsHandler.impl.CaptionsMediaItemImpl;
import org.opencastproject.media.mediapackage.MediaPackage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * This defines the entities for the SOAP/REST services
 */

@XmlType(name="captions-entity", namespace="http://captions.opencastproject.org/")
@XmlRootElement(name="captions-entity", namespace="http://captions.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class CaptionsEntityJaxb {
  private static final Logger logger = LoggerFactory.getLogger(CaptionsEntityJaxb.class);

  public CaptionsEntityJaxb(CaptionsMediaItem entity) {
    logger.info("Creating a " + CaptionsEntityJaxb.class.getName() + " from " + entity);
    this.mp = entity.getMediaPackage();
    this.workflowId = entity.getWorkflowId();
    this.mediaPackageId = entity.getMediaPackage().getIdentifier().compact();
    this.id = this.workflowId;
  }

  @XmlTransient
  public CaptionsMediaItem getEntity() {
    CaptionsMediaItem entity = new CaptionsMediaItemImpl(id, mp);
    return entity;
  }

  @XmlID
  @XmlAttribute()
  private String id;

  @XmlElement(name="mediaPackageId")
  private String mediaPackageId;

  @XmlElement(name="workflowId")
  private String workflowId;

  private MediaPackage mp;

  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }

}

