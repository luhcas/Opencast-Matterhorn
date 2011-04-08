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
package org.opencastproject.userdirectory.jpa;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Models the relationship between a local and an application role.
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "MH_ROLE_MAPPING")
@NamedQuery(name = "role", query = "select mapping from JpaRoleMapping mapping where mapping.applicationRole = :applicationRole")
public class JpaRoleMapping {

  /** The java.io.serialization uid */
  private static final long serialVersionUID = -6693877536928844019L;

  @Id
  @Column(name = "app")
  protected String applicationRole;

  @Column(name = "local")
  protected String localRole;

  /**
   * No-arg constructor needed by JPA
   */
  public JpaRoleMapping() {
  }

  /**
   * Constructs a role mapping with the specified username, password, and roles.
   * 
   * @param applicationRole
   *          the application role
   * @param localRole
   *          the local role
   */
  public JpaRoleMapping(String applicationRole, String localRole) {
    super();
    this.applicationRole = applicationRole;
    this.localRole = localRole;
  }

  /**
   * @return the applicationRole
   */
  public String getApplicationRole() {
    return applicationRole;
  }

  /**
   * @param applicationRole
   *          the applicationRole to set
   */
  public void setApplicationRole(String applicationRole) {
    this.applicationRole = applicationRole;
  }

  /**
   * @return the localRole
   */
  public String getLocalRole() {
    return localRole;
  }

  /**
   * @param localRole
   *          the localRole to set
   */
  public void setLocalRole(String localRole) {
    this.localRole = localRole;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "{" + applicationRole + "=" + localRole + "}";
  }

}
