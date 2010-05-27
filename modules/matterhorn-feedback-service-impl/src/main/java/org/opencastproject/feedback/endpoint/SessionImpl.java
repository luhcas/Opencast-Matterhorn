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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.opencastproject.feedback.api.Session;

/**
 * A JAXB-annotated implementation of {@link Session}
 */
@Entity(name = "SessionImpl")
@Table(name = "MH_SESSION_IMPL")
@NamedQueries( {
        @NamedQuery(name = "findSessions", query = "SELECT s FROM SessionImpl s")})
@XmlType(name = "session", namespace = "http://feedback.opencastproject.org/")
@XmlRootElement(name = "session", namespace = "http://feedback.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class SessionImpl implements Session {

  @Id
  @Column(name = "session_id")
  @XmlAttribute(name = "id")
  private String sessionId;

  @Column(name = "user_id")
  @XmlElement(name = "user-id")
  private String userId;

  /**
   * A no-arg constructor needed by JAXB
   */
  public SessionImpl() {
  }

  public String getSessionId(){
    return sessionId;
  }

  public void setSessionId(String sessionId){
    this.sessionId = sessionId;
  }

  public String getUserId(){
    return userId;
  }

  public void setUserId(String userId){
    this.userId = userId;
  }

}
