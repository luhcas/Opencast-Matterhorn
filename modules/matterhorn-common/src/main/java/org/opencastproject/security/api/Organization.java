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
package org.opencastproject.security.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * An organization that is hosted on this Matterhorn instance.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "org", namespace = "org.opencastproject.security")
@XmlRootElement(name = "org", namespace = "org.opencastproject.security")
public class Organization {

  /** The organizational identifier */
  protected String id = null;

  /** The friendly name of the organization */
  protected String name = null;

  /** The host name */
  protected String serverName = null;

  /** The host port */
  protected int serverPort = 80;

  /** The local admin role name */
  protected String adminRole = null;

  /** The local anonymous role name */
  protected String anonymousRole = null;

  /**
   * No-arg constructor needed by JAXB
   */
  public Organization() {
  }

  /**
   * Constructs an organization with its attributes and a default server port of <code>80</code>.
   * 
   * @param id
   *          the unique identifier
   * @param name
   *          the friendly name
   * @param serverName
   *          the host name
   * @param adminRole
   *          name of the local admin role
   * @param anonymousRole
   *          name of the local anonymous role
   */
  public Organization(String id, String name, String serverName, String adminRole, String anonymousRole) {
    this(id, name, serverName, 80, adminRole, anonymousRole);
  }

  /**
   * Constructs an organization with its attributes.
   * 
   * @param id
   *          the unique identifier
   * @param name
   *          the friendly name
   * @param serverName
   *          the host name
   * @param serverPort
   *          the host port
   * @param adminRole
   *          name of the local admin role
   * @param anonymousRole
   *          name of the local anonymous role
   */
  public Organization(String id, String name, String serverName, int serverPort, String adminRole, String anonymousRole) {
    this.id = id;
    this.name = name;
    this.serverName = serverName;
    this.serverPort = serverPort;
    this.adminRole = adminRole;
    this.anonymousRole = anonymousRole;
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the serverName
   */
  public String getServerName() {
    return serverName;
  }

  /**
   * @return the serverPort
   */
  public int getServerPort() {
    return serverPort;
  }

  /**
   * Returns the name for the local admin role.
   * 
   * @return the admin role name
   */
  public String getAdminRole() {
    return adminRole;
  }

  /**
   * Returns the name for the local anonymous role.
   * 
   * @return the anonymous role name
   */
  public String getAnonymousRole() {
    return anonymousRole;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return id;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Organization))
      return false;
    return ((Organization) obj).id.equals(id);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return id.hashCode();
  }

}
