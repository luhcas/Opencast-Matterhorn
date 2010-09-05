/**
 *  Copyright 2009, 2010 The Regents of the University of California
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
package org.opencastproject.feedback.endpoint;

import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.opencastproject.feedback.api.Annotation;

/**
 * A JAXB-annotated implementation of {@link Annotation}
 */
@Entity(name = "AnnotationImpl")
@Table(name = "ANNOTATION")
@NamedQueries( {
        @NamedQuery(name = "findAnnotations", query = "SELECT a FROM AnnotationImpl a"),
        @NamedQuery(name = "countSessionsGroupByMediapackage", query = "SELECT a.mediapackageId, COUNT(distinct a.sessionId), SUM(a.length) FROM AnnotationImpl a GROUP BY a.mediapackageId"),
        @NamedQuery(name = "countSessionsGroupByMediapackageByIntervall", query = "SELECT a.mediapackageId, COUNT(distinct a.sessionId), SUM(a.length) FROM AnnotationImpl a WHERE :begin <= a.created AND a.created <= :end GROUP BY a.mediapackageId"),
        @NamedQuery(name = "countSessionsOfMediapackage", query = "SELECT COUNT(distinct a.sessionId) FROM AnnotationImpl a WHERE a.mediapackageId = :mediapackageId"),
        @NamedQuery(name = "findLastAnnotationsOfSession", query = "SELECT a FROM AnnotationImpl a  WHERE a.sessionId = :sessionId ORDER BY a.created DESC"),
        @NamedQuery(name = "findAnnotationsByKey", query = "SELECT a FROM AnnotationImpl a WHERE a.key = :key"),
        @NamedQuery(name = "findAnnotationsByKeyAndMediapackageId", query = "SELECT a FROM AnnotationImpl a WHERE a.mediapackageId = :mediapackageId AND a.key = :key"),
        @NamedQuery(name = "findAnnotationsByKeyAndMediapackageIdOrderByOutpointDESC", query = "SELECT a FROM AnnotationImpl a WHERE a.mediapackageId = :mediapackageId AND a.key = :key ORDER BY a.outpoint DESC"),
        @NamedQuery(name = "findAnnotationsByIntervall", query = "SELECT a FROM AnnotationImpl a WHERE :begin <= a.created AND a.created <= :end"),
        @NamedQuery(name = "findAnnotationsByKeyAndIntervall", query = "SELECT a FROM AnnotationImpl a WHERE :begin <= a.created AND a.created <= :end AND a.key = :key"),
        @NamedQuery(name = "findTotal", query = "SELECT COUNT(a) FROM AnnotationImpl a"),
        @NamedQuery(name = "findTotalByKey", query = "SELECT COUNT(a) FROM AnnotationImpl a WHERE a.key = :key"),
        @NamedQuery(name = "findTotalByKeyAndMediapackageId", query = "SELECT COUNT(a) FROM AnnotationImpl a WHERE a.mediapackageId = :mediapackageId AND a.key = :key"),
        @NamedQuery(name = "findTotalByIntervall", query = "SELECT COUNT(a) FROM AnnotationImpl a WHERE :begin <= a.created AND a.created <= :end"),
        @NamedQuery(name = "findTotalByKeyAndIntervall", query = "SELECT COUNT(a) FROM AnnotationImpl a WHERE :begin <= a.created AND a.created <= :end AND a.key = :key") })
@XmlType(name = "annotation", namespace = "http://feedback.opencastproject.org/")
@XmlRootElement(name = "annotation", namespace = "http://feedback.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class AnnotationImpl implements Annotation {

  @Id
  @Column(name="ID")
  @GeneratedValue(strategy = GenerationType.AUTO)
  @XmlElement(name = "annotation-id")
  private int annotationId;

  @Column(name = "MEDIA_PACKAGE_ID")
  @XmlElement(name = "mediapackage-id")
  private String mediapackageId;

  @Column(name = "USER_ID")
  @XmlElement(name = "user-id")
  private String userId;

  @Column(name = "SESSION_ID")
  @XmlElement(name = "session-id")
  private String sessionId;

  @Column(name = "INPOINT")
  @XmlElement(name = "inpoint")
  private int inpoint;

  @Column(name = "OUTPOINT")
  @XmlElement(name = "outpoint")
  private int outpoint;

  @Column(name = "LENGTH")
  @XmlElement(name = "length")
  private int length;

  @Column(name = "ANNOTATION_KEY")
  @XmlElement(name = "key")
  private String key;

  @Column(name = "ANNOTATION_VAL")
  @XmlElement(name = "value")
  private String value;

  @Basic(optional = false)
  @Column(name = "CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  @XmlElement(name = "created")
  private Date created = new Date();

  /**
   * A no-arg constructor needed by JAXB
   */
  public AnnotationImpl() {
  }

  public int getAnnotationId() {
    return annotationId;
  }

  public void setAnnotationId(int annotationId) {
    this.annotationId = annotationId;
  }

  public String getMediapackageId() {
    return mediapackageId;
  }

  public void setMediapackageId(String mediapackageId) {
    this.mediapackageId = mediapackageId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public int getInpoint() {
    return inpoint;
  }

  public void setInpoint(int inpoint) {
    this.inpoint = inpoint;
    updateLength();
  }

  public int getOutpoint() {
    return outpoint;
  }

  public void setOutpoint(int outpoint) {
    this.outpoint = outpoint;
    updateLength();
  }

  public int getLength() {
    return length;
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

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  private void updateLength() {
    this.length = this.outpoint - this.inpoint;
  }
}
